# Remix Studio

A dark web interface around `remix.py`, the slowed + pitched-down + reverb + bass-boost remix generator.

- Search SoundCloud (or paste a track link) and pick a song.
- Adjust every stage of the remix chain with sliders, or start from a preset.
- Switch the interface between English and Russian with the EN / RU buttons (remembered per browser).
- Render a 45-second preview or the full track, listen in the built-in player,
  flip between remix and original at the same spot, and download the MP3.

## Run

Requires Python 3.10+ and `ffmpeg` on the PATH.

```bash
python -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn server:app --port 3000
```

Open http://localhost:3000.

On Windows, double-click `run.bat` instead: it sets everything up on first run, starts the server and
opens the browser. Python and `ffmpeg` still need to be installed first; the script tells you if either is missing.

## How it fits together

| File | Role |
| --- | --- |
| `remix.py` | The audio chain and its CLI (`python remix.py input.mp3 output.mp3`). Unchanged; its argparse defaults are the UI defaults. |
| `server.py` | FastAPI app: SoundCloud search and download via `yt-dlp`, a one-at-a-time render queue, audio serving. |
| `static/` | The interface (plain HTML, CSS and JavaScript, no build step). |

Downloaded tracks and renders are cached in `.cache/` (override with `REMIX_DATA_DIR`); only the 30 most
recent files of each are kept. Tracks longer than 15 minutes are rejected because the chain processes the
whole track in memory.

Notes:

- Only `https://soundcloud.com` links are accepted.
- Tracks that SoundCloud restricts to subscribers come through as 30-second snippets.
- Only remix music you have the rights to use.
