"""Web interface for remix.py: pick a SoundCloud track, tweak the chain, listen.

    uvicorn server:app --port 3000
"""
import argparse
import asyncio
import hashlib
import json
import os
import subprocess
import tempfile
import threading
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from urllib.parse import urlparse

import numpy as np
import yt_dlp
from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pedalboard.io import AudioFile
from pydantic import BaseModel

import remix

ROOT = Path(__file__).parent
DATA = Path(os.environ.get("REMIX_DATA_DIR", ROOT / ".cache"))
TRACKS = DATA / "tracks"
RENDERS = DATA / "renders"
for d in (TRACKS, RENDERS):
    d.mkdir(parents=True, exist_ok=True)

MAX_DURATION = 15 * 60  # seconds; the chain holds the whole track in memory
MAX_CACHED_FILES = 30
MAX_UPLOAD_BYTES = 200 * 1024 * 1024
UPLOAD_PREFIX = "upload:"
PREVIEW_LENGTH = 45.0
PREVIEW_OFFSET = 30.0
WAVEFORM_POINTS = 400
SOUNDCLOUD_HOSTS = {"soundcloud.com", "www.soundcloud.com", "m.soundcloud.com", "on.soundcloud.com"}
BITRATES = [128, 192, 256, 320]

# UI ranges per remix.py option: (key, label, min, max, step, unit, advanced)
GROUPS = [
    ("Tempo & pitch", [
        ("speed", "Speed", 0.5, 1.5, 0.001, "×", False),
        ("semitones", "Pitch", -12, 12, 0.01, "st", False),
    ]),
    ("Tone", [
        ("bass", "Bass boost", -6, 24, 0.5, "dB", False),
        ("presence", "Presence", -6, 12, 0.5, "dB", False),
        ("air", "Air", -6, 15, 0.5, "dB", False),
        ("drive", "Saturation", 0, 12, 0.5, "dB", False),
        ("highpass", "Sub cut", 0, 200, 1, "Hz", True),
        ("bass_freq", "Bass corner", 40, 400, 5, "Hz", True),
        ("presence_freq", "Presence centre", 500, 8000, 50, "Hz", True),
        ("air_freq", "Air corner", 4000, 18000, 100, "Hz", True),
    ]),
    ("Reverb", [
        ("reverb_mix", "Mix", 0, 1, 0.01, "", False),
        ("reverb_size", "Room size", 0, 1, 0.01, "", False),
        ("reverb_damping", "Damping", 0, 1, 0.01, "", False),
        ("reverb_width", "Stereo width", 0, 2, 0.01, "", True),
        ("reverb_lowcut", "Wet low cut", 20, 500, 5, "Hz", True),
        ("mono_below", "Wet mono below", 0, 500, 10, "Hz", True),
        ("reverb_tail", "Tail", 0, 10, 0.5, "s", True),
    ]),
    ("Dynamics", [
        ("loudness", "Loudness", -30, -6, 0.1, "dBFS", False),
        ("ceiling", "Peak ceiling", -12, 0, 0.1, "dBFS", False),
        ("comp_threshold", "Comp threshold", -50, 0, 0.5, "dB", True),
        ("comp_ratio", "Comp ratio", 1, 20, 0.1, ":1", True),
    ]),
]

# remix.py stays the single source of truth for defaults.
DEFAULTS = vars(remix.build_parser().parse_args(["in", "out"]))
RANGES = {f[0]: (f[2], f[3]) for _, fields in GROUPS for f in fields}

app = FastAPI(title="Remix Studio")
jobs: dict[str, dict] = {}
jobs_lock = threading.Lock()
download_locks: dict[str, threading.Lock] = {}
# One render at a time: each holds a full decoded track in memory.
pool = ThreadPoolExecutor(max_workers=1)


def soundcloud_url(url: str) -> str:
    """Only ever hand SoundCloud URLs to yt-dlp."""
    parsed = urlparse(url.strip())
    if parsed.scheme != "https" or parsed.hostname not in SOUNDCLOUD_HOSTS:
        raise HTTPException(400, "Only https://soundcloud.com links are supported")
    return parsed.geturl()


def looks_like_url(text: str) -> bool:
    return text.startswith(("http://", "https://")) or text.startswith(("soundcloud.com/", "on.soundcloud.com/"))


def track_summary(e: dict) -> dict | None:
    url = e.get("webpage_url") or e.get("url")
    if not url or urlparse(url).hostname not in SOUNDCLOUD_HOSTS:
        return None
    thumbs = {t.get("id"): t.get("url") for t in e.get("thumbnails") or []}
    art = thumbs.get("t300x300") or thumbs.get("large") or e.get("thumbnail")
    if not art and thumbs:
        art = list(thumbs.values())[-1]
    return {
        "id": str(e.get("id")),
        "title": e.get("title") or "Untitled",
        "artist": e.get("uploader") or "",
        "duration": e.get("duration"),
        "url": url,
        "artwork": art,
    }


def ydl(**opts):
    return yt_dlp.YoutubeDL({"quiet": True, "no_warnings": True, "noprogress": True, **opts})


@app.get("/api/config")
def config():
    return {
        "groups": [
            {
                "title": title,
                "fields": [
                    dict(key=k, label=label, min=lo, max=hi, step=step, unit=unit,
                         advanced=adv, default=DEFAULTS[k])
                    for k, label, lo, hi, step, unit, adv in fields
                ],
            }
            for title, fields in GROUPS
        ],
        "bitrates": BITRATES,
        "defaultBitrate": DEFAULTS["bitrate"],
        "previewLength": PREVIEW_LENGTH,
        "maxDuration": MAX_DURATION,
    }


@app.get("/api/search")
def search(q: str = Query(min_length=1, max_length=300)):
    q = q.strip()
    try:
        if looks_like_url(q):
            url = soundcloud_url(q if q.startswith("http") else "https://" + q)
            with ydl(extract_flat="in_playlist", playlistend=25) as y:
                info = y.extract_info(url, download=False)
        else:
            with ydl(extract_flat=True) as y:
                info = y.extract_info(f"scsearch12:{q}", download=False)
    except yt_dlp.utils.DownloadError as exc:
        raise HTTPException(502, f"SoundCloud lookup failed: {clean_error(exc)}")
    entries = info.get("entries") if info.get("_type") == "playlist" else [info]
    tracks = [t for t in (track_summary(e) for e in entries or [] if e) if t]
    return {"tracks": tracks}


def clean_error(exc: Exception) -> str:
    return str(exc).removeprefix("ERROR: ").strip()[:300]


def fetch_track(url: str, job: dict) -> Path:
    """Download the track as MP3 (cached per URL) and return its path."""
    key = hashlib.sha1(url.encode()).hexdigest()[:16]
    path = TRACKS / f"{key}.mp3"
    with jobs_lock:
        lock = download_locks.setdefault(key, threading.Lock())
    with lock:
        if path.exists():
            os.utime(path)
            return path
        job["status"] = "downloading"
        opts = dict(
            format="bestaudio[acodec=mp3]/bestaudio/best",
            outtmpl=str(TRACKS / f"{key}.%(ext)s"),
            noplaylist=True,
            postprocessors=[{"key": "FFmpegExtractAudio", "preferredcodec": "mp3", "preferredquality": "192"}],
        )
        with ydl(**opts) as y:
            info = y.extract_info(url, download=False)
            if info.get("_type") == "playlist":
                raise ValueError("That link is a playlist; pick a single track")
            if (info.get("duration") or 0) > MAX_DURATION:
                raise ValueError(f"Track is longer than {MAX_DURATION // 60} minutes")
            y.process_ie_result(info, download=True)
        if not path.exists():
            raise ValueError("SoundCloud did not return a playable stream for this track")
        prune(TRACKS)
        return path


def prune(folder: Path):
    files = sorted(folder.glob("*.mp3"), key=lambda p: p.stat().st_mtime, reverse=True)
    for old in files[MAX_CACHED_FILES:]:
        old.unlink(missing_ok=True)


def waveform(audio: np.ndarray) -> list[float]:
    mono = np.abs(audio).max(axis=0)
    size = max(1, len(mono) // WAVEFORM_POINTS)
    usable = mono[: size * (len(mono) // size)]
    if not len(usable):
        return []
    peaks = usable.reshape(-1, size).max(axis=1)
    return np.round(peaks / max(float(peaks.max()), 1e-9), 3).tolist()


def render(job: dict, url: str, settings: dict, preview: bool):
    try:
        if url.startswith(UPLOAD_PREFIX):
            src = TRACKS / f"{url.removeprefix(UPLOAD_PREFIX)}.mp3"
            os.utime(src)
        else:
            src = fetch_track(url, job)
        job["status"] = "processing"
        with AudioFile(str(src)) as f:
            sr = int(f.samplerate)
            total = f.frames / sr
            start = 0.0
            frames = f.frames
            if preview and total > PREVIEW_LENGTH:
                start = min(PREVIEW_OFFSET, total - PREVIEW_LENGTH)
                f.seek(int(start * sr))
                frames = int(PREVIEW_LENGTH * sr)
            audio = f.read(frames)
        if audio.shape[1] == 0:
            raise ValueError("Downloaded track contains no audio")
        out = remix.remix(audio, sr, argparse.Namespace(**settings))
        out = np.ascontiguousarray(out, dtype=np.float32)
        job["status"] = "encoding"
        dst = RENDERS / f"{job['id']}.mp3"
        tmp = dst.with_suffix(".part.mp3")
        with AudioFile(str(tmp), "w", sr, out.shape[0], quality=settings["bitrate"]) as f:
            f.write(out)
        tmp.replace(dst)
        job.update(
            status="done",
            duration=out.shape[1] / sr,
            sourceStart=start,
            sourceDuration=audio.shape[1] / sr,
            peaks=waveform(out),
            audio=f"/api/renders/{job['id']}",
            original=f"/api/tracks/{src.stem}",
        )
        prune(RENDERS)
    except Exception as exc:  # surfaced to the UI rather than lost in the worker
        job.update(status="error", error=clean_error(exc))


def transcode_upload(tmp: Path, key: str) -> float:
    """Convert an uploaded file to the cached MP3 format; returns its duration."""
    dst = TRACKS / f"{key}.mp3"
    if not dst.exists():
        part = TRACKS / f"{key}.part.mp3"
        cmd = ["ffmpeg", "-v", "error", "-y", "-i", str(tmp), "-vn", "-map", "0:a:0",
               "-c:a", "libmp3lame", "-b:a", "320k", str(part)]
        if subprocess.run(cmd, capture_output=True).returncode != 0:
            part.unlink(missing_ok=True)
            raise HTTPException(400, "File is not a supported audio file")
        part.replace(dst)
    with AudioFile(str(dst)) as f:
        duration = f.frames / f.samplerate
    if duration > MAX_DURATION:
        dst.unlink(missing_ok=True)
        raise HTTPException(400, f"Track is longer than {MAX_DURATION // 60} minutes")
    os.utime(dst)
    prune(TRACKS)
    return duration


@app.post("/api/upload")
async def upload(request: Request, name: str = Query("track", max_length=300)):
    """Accept a local audio file (raw request body) as the track to remix."""
    too_large = HTTPException(413, f"File is too large (max {MAX_UPLOAD_BYTES // 2**20} MB)")
    if int(request.headers.get("content-length") or 0) > MAX_UPLOAD_BYTES:
        raise too_large
    digest = hashlib.sha1()
    size = 0
    with tempfile.NamedTemporaryFile(dir=TRACKS, suffix=".upload", delete=False) as tmp:
        tmp_path = Path(tmp.name)
        try:
            async for chunk in request.stream():
                size += len(chunk)
                if size > MAX_UPLOAD_BYTES:
                    raise too_large
                digest.update(chunk)
                tmp.write(chunk)
            tmp.flush()
            key = digest.hexdigest()[:16]
            duration = await asyncio.to_thread(transcode_upload, tmp_path, key)
        finally:
            tmp.close()
            tmp_path.unlink(missing_ok=True)
    return {
        "id": key,
        "title": Path(name).stem[:120] or "Track",
        "artist": "",
        "duration": duration,
        "url": f"{UPLOAD_PREFIX}{key}",
        "artwork": None,
    }


class RemixRequest(BaseModel):
    url: str
    settings: dict[str, float] = {}
    bitrate: int = DEFAULTS["bitrate"]
    preview: bool = True


@app.post("/api/remix")
def start_remix(req: RemixRequest):
    if req.url.startswith(UPLOAD_PREFIX):
        key = req.url.removeprefix(UPLOAD_PREFIX)
        if not key.isalnum() or not (TRACKS / f"{key}.mp3").exists():
            raise HTTPException(404, "Uploaded file expired; upload it again")
        url = req.url
    else:
        url = soundcloud_url(req.url)
    settings = {}
    for key, (lo, hi) in RANGES.items():
        value = float(req.settings.get(key, DEFAULTS[key]))
        if not np.isfinite(value):
            raise HTTPException(400, f"Invalid value for {key}")
        settings[key] = min(max(value, lo), hi)
    settings["bitrate"] = req.bitrate if req.bitrate in BITRATES else DEFAULTS["bitrate"]

    # Identical requests share one render.
    digest = json.dumps([url, settings, req.preview], sort_keys=True)
    job_id = hashlib.sha1(digest.encode()).hexdigest()[:20]
    with jobs_lock:
        job = jobs.get(job_id)
        stale = job and job["status"] == "done" and not (RENDERS / f"{job_id}.mp3").exists()
        if job is None or stale or job["status"] == "error":
            job = jobs[job_id] = {"id": job_id, "status": "queued", "preview": req.preview}
            pool.submit(render, job, url, settings, req.preview)
    return job


@app.get("/api/jobs/{job_id}")
def job_status(job_id: str):
    job = jobs.get(job_id)
    if not job:
        raise HTTPException(404, "Unknown job")
    return job


def audio_file(folder: Path, name: str, download: str | None):
    if not name.isalnum():
        raise HTTPException(404)
    path = folder / f"{name}.mp3"
    if not path.exists():
        raise HTTPException(404, "Audio expired; render again")
    filename = None
    if download:
        safe = "".join(c for c in download if c.isalnum() or c in " -_()").strip()[:80] or "remix"
        filename = f"{safe}.mp3"
    return FileResponse(path, media_type="audio/mpeg", filename=filename)


@app.get("/api/renders/{name}")
def get_render(name: str, download: str | None = None):
    return audio_file(RENDERS, name, download)


@app.get("/api/tracks/{name}")
def get_track(name: str):
    return audio_file(TRACKS, name, None)


app.mount("/", StaticFiles(directory=ROOT / "static", html=True), name="static")
