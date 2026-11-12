const $ = (id) => document.getElementById(id);

const PRESETS = [
  { name: "Slowed + Reverb", values: {} },
  { name: "Gentle", values: { speed: 0.92, semitones: -1.5, bass: 6, presence: 2, air: 3, drive: 1, reverb_mix: 0.2 } },
  { name: "Cathedral", values: { speed: 0.8, semitones: -4, reverb_mix: 0.6, reverb_size: 0.95, reverb_damping: 0.2, reverb_tail: 4 } },
  { name: "Bass boost", values: { speed: 1, semitones: 0, bass: 12, presence: 2, air: 2, drive: 2, reverb_mix: 0 } },
  { name: "Nightcore", values: { speed: 1.25, semitones: 3.86, bass: 3, presence: 2, air: 3, drive: 0, reverb_mix: 0.08, reverb_size: 0.4 } },
];

const state = {
  fields: {},      // key -> { def, range, number, row }
  defaults: {},
  track: null,
  preview: true,
  result: null,    // finished job plus the speed it was rendered with
  renderedKey: null,
  source: "remix",
  requestId: 0,
};

const audio = { remix: $("audio-remix"), original: $("audio-original") };

function formatTime(seconds) {
  if (!Number.isFinite(seconds) || seconds < 0) seconds = 0;
  const s = Math.floor(seconds);
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
}

function decimals(step) {
  return (String(step).split(".")[1] || "").length;
}

async function api(path, options) {
  const res = await fetch(path, options);
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(typeof body.detail === "string" ? body.detail : `Request failed (${res.status})`);
  return body;
}

/* ---------- settings ---------- */

function setField(key, value, { silent = false } = {}) {
  const f = state.fields[key];
  const { min, max, step } = f.def;
  if (!Number.isFinite(value)) value = f.def.default;
  value = Math.min(max, Math.max(min, value));
  f.value = value;
  f.range.value = value;
  f.number.value = value.toFixed(decimals(step));
  f.range.style.setProperty("--fill", `${((value - min) / (max - min)) * 100}%`);
  f.row.classList.toggle("changed", Math.abs(value - f.def.default) > 1e-9);
  if (!silent) settingsChanged();
}

function currentSettings() {
  return Object.fromEntries(Object.entries(state.fields).map(([k, f]) => [k, f.value]));
}

function requestKey() {
  return JSON.stringify([state.track && state.track.url, currentSettings(), $("bitrate").value, state.preview]);
}

function settingsChanged() {
  const settings = currentSettings();
  for (const chip of $("presets").children) {
    const target = { ...state.defaults, ...PRESETS[chip.dataset.index].values };
    const match = Object.keys(target).every((k) => Math.abs(target[k] - settings[k]) < 1e-9);
    chip.setAttribute("aria-pressed", match);
  }
  if (state.result && state.renderedKey !== requestKey() && !$("status").classList.contains("busy")) {
    setStatus("Settings changed — render again to hear them.");
  }
}

function buildSettings(config) {
  for (const group of config.groups) {
    const box = document.createElement("div");
    box.className = "group";
    const h = document.createElement("h3");
    h.textContent = group.title;
    box.append(h);
    for (const def of group.fields) {
      state.defaults[def.key] = def.default;
      const row = document.createElement("div");
      row.className = "field" + (def.advanced ? " advanced" : "");
      const label = document.createElement("label");
      label.textContent = def.label;
      label.htmlFor = `f-${def.key}`;
      const value = document.createElement("div");
      value.className = "value";
      const number = document.createElement("input");
      number.type = "number";
      number.setAttribute("aria-label", `${def.label} value`);
      const unit = document.createElement("span");
      unit.textContent = def.unit;
      value.append(number, unit);
      const range = document.createElement("input");
      range.type = "range";
      range.id = `f-${def.key}`;
      for (const input of [number, range]) {
        input.min = def.min;
        input.max = def.max;
        input.step = def.step;
      }
      range.addEventListener("input", () => setField(def.key, parseFloat(range.value)));
      number.addEventListener("change", () => setField(def.key, parseFloat(number.value)));
      range.addEventListener("dblclick", () => setField(def.key, def.default));
      range.title = "Double-click to reset";
      row.append(label, value, range);
      box.append(row);
      state.fields[def.key] = { def, range, number, row };
      setField(def.key, def.default, { silent: true });
    }
    $("groups").append(box);
  }

  PRESETS.forEach((preset, index) => {
    const chip = document.createElement("button");
    chip.type = "button";
    chip.className = "chip";
    chip.textContent = preset.name;
    chip.dataset.index = index;
    chip.addEventListener("click", () => {
      const target = { ...state.defaults, ...preset.values };
      for (const key of Object.keys(target)) setField(key, target[key], { silent: true });
      settingsChanged();
    });
    $("presets").append(chip);
  });

  for (const rate of config.bitrates) {
    const option = new Option(`${rate} kbps`, rate, false, rate === config.defaultBitrate);
    $("bitrate").append(option);
  }
  $("bitrate").addEventListener("change", settingsChanged);
  state.maxDuration = config.maxDuration;
  $("mode-preview").textContent = `Preview ${config.previewLength}s`;
  settingsChanged();
}

/* ---------- search ---------- */

function artStyle(el, url) {
  el.style.backgroundImage = url ? `url("${encodeURI(url)}")` : "";
}

function renderResults(tracks) {
  const list = $("results");
  list.replaceChildren();
  if (!tracks.length) {
    showHint("No tracks found. Try another search.");
    return;
  }
  for (const track of tracks) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "track";
    button.setAttribute("aria-pressed", "false");
    const art = document.createElement("span");
    art.className = "art";
    artStyle(art, track.artwork);
    const text = document.createElement("span");
    text.style.minWidth = 0;
    const title = document.createElement("span");
    title.className = "title";
    title.textContent = track.title;
    const artist = document.createElement("span");
    artist.className = "artist";
    artist.textContent = track.artist;
    text.append(title, artist);
    const dur = document.createElement("span");
    dur.className = "dur";
    dur.textContent = track.duration ? formatTime(track.duration) : "";
    button.append(art, text, dur);
    if (track.duration > state.maxDuration) {
      button.disabled = true;
      button.title = `Longer than ${Math.round(state.maxDuration / 60)} minutes`;
    }
    button.addEventListener("click", () => selectTrack(track, button));
    list.append(button);
  }
}

function showHint(message, isError = false) {
  const p = document.createElement("p");
  p.className = "hint" + (isError ? " error" : "");
  p.textContent = message;
  $("results").replaceChildren(p);
}

let searchId = 0;
async function search(query) {
  const id = ++searchId;
  $("results").replaceChildren(...Array.from({ length: 6 }, () => {
    const s = document.createElement("div");
    s.className = "skeleton";
    return s;
  }));
  try {
    const { tracks } = await api(`/api/search?q=${encodeURIComponent(query)}`);
    if (id !== searchId) return;
    renderResults(tracks);
    if (tracks.length === 1) selectTrack(tracks[0], $("results").firstElementChild);
  } catch (err) {
    if (id === searchId) showHint(err.message, true);
  }
}

function selectTrack(track, button) {
  state.track = track;
  for (const el of $("results").children) el.setAttribute("aria-pressed", el === button);
  $("now-title").textContent = track.title;
  $("now-artist").textContent = [track.artist, track.duration && formatTime(track.duration)].filter(Boolean).join(" · ");
  artStyle($("now-art"), track.artwork);
  $("render").disabled = false;
  setStatus("Ready — tweak the settings and render.");
}

/* ---------- rendering ---------- */

function setStatus(message, kind = "") {
  const el = $("status");
  el.textContent = message;
  el.className = "status" + (kind ? ` ${kind}` : "");
}

const STATUS_TEXT = {
  queued: "Waiting for the renderer…",
  downloading: "Fetching the track from SoundCloud…",
  processing: "Remixing — stretching, EQ, reverb…",
  encoding: "Encoding MP3…",
};

async function render() {
  if (!state.track) return;
  const id = ++state.requestId;
  const track = state.track;
  const settings = currentSettings();
  const key = requestKey();
  $("render").disabled = true;
  setStatus(STATUS_TEXT.queued, "busy");
  try {
    let job = await api("/api/remix", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url: track.url, settings, bitrate: Number($("bitrate").value), preview: state.preview }),
    });
    while (job.status !== "done" && job.status !== "error") {
      if (id !== state.requestId) return;
      setStatus(STATUS_TEXT[job.status] || "Working…", "busy");
      await new Promise((r) => setTimeout(r, 700));
      job = await api(`/api/jobs/${job.id}`);
    }
    if (id !== state.requestId) return;
    if (job.status === "error") throw new Error(job.error || "Render failed");
    state.renderedKey = key;
    loadResult({ ...job, speed: settings.speed, track });
    setStatus(job.preview && job.sourceStart > 0
      ? `Preview ready — starts ${formatTime(job.sourceStart)} into the track.`
      : "Remix ready.");
  } catch (err) {
    if (id === state.requestId) setStatus(err.message, "error");
  } finally {
    if (id === state.requestId) $("render").disabled = !state.track;
  }
}

/* ---------- player ---------- */

function loadResult(result) {
  pause();
  state.result = result;
  state.source = "remix";
  audio.remix.src = result.audio;
  audio.original.src = result.original;
  $("play").disabled = false;
  $("player-title").textContent = result.track.title;
  $("time-total").textContent = formatTime(result.duration);
  for (const b of document.querySelectorAll(".ab button")) b.disabled = false;
  const link = $("download");
  link.href = `${result.audio}?download=${encodeURIComponent(`${result.track.title} (remix)`)}`;
  link.classList.remove("disabled");
  link.removeAttribute("aria-disabled");
  $("wave").setAttribute("aria-valuemax", Math.round(result.duration));
  updateSource();
  draw();
  play();
}

// The timeline is always in remix time; the original is mapped onto it.
function getTime() {
  const r = state.result;
  if (!r) return 0;
  if (state.source === "remix") return audio.remix.currentTime;
  return Math.max(0, (audio.original.currentTime - r.sourceStart) / r.speed);
}

function setTime(t) {
  const r = state.result;
  if (!r) return;
  t = Math.min(Math.max(t, 0), r.duration);
  if (state.source === "remix") audio.remix.currentTime = t;
  else audio.original.currentTime = r.sourceStart + Math.min(t * r.speed, r.sourceDuration);
  draw();
}

function active() {
  return audio[state.source];
}

function play() {
  active().play().catch(() => {});
}

function pause() {
  audio.remix.pause();
  audio.original.pause();
}

function updateSource() {
  for (const b of document.querySelectorAll(".ab button")) {
    b.setAttribute("aria-checked", b.dataset.source === state.source);
  }
  const r = state.result;
  $("player-sub").textContent = state.source === "remix"
    ? `Remix${r && r.preview ? " preview" : ""}`
    : "Original (same section)";
}

function switchSource(source) {
  if (!state.result || source === state.source) return;
  const t = getTime();
  const wasPlaying = !active().paused;
  pause();
  state.source = source;
  setTime(t);
  updateSource();
  if (wasPlaying) play();
}

function draw() {
  const canvas = $("wave");
  const ratio = window.devicePixelRatio || 1;
  const width = canvas.clientWidth;
  const height = canvas.clientHeight;
  if (canvas.width !== Math.round(width * ratio) || canvas.height !== Math.round(height * ratio)) {
    canvas.width = Math.round(width * ratio);
    canvas.height = Math.round(height * ratio);
  }
  const ctx = canvas.getContext("2d");
  ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
  ctx.clearRect(0, 0, width, height);

  const r = state.result;
  const t = getTime();
  const progress = r ? Math.min(1, t / r.duration) : 0;
  const peaks = r && r.peaks.length ? r.peaks : null;
  const barWidth = 3;
  const count = Math.max(1, Math.floor(width / barWidth));
  const gradient = ctx.createLinearGradient(0, 0, width, 0);
  gradient.addColorStop(0, "#a78bfa");
  gradient.addColorStop(1, "#f472b6");

  for (let i = 0; i < count; i++) {
    let peak = 0.06;
    if (peaks) {
      const from = Math.floor((i / count) * peaks.length);
      const to = Math.max(from + 1, Math.floor(((i + 1) / count) * peaks.length));
      peak = Math.max(0.06, ...peaks.slice(from, to));
    }
    const h = Math.max(2, peak * (height - 4));
    ctx.fillStyle = (i + 0.5) / count <= progress ? gradient : "#2e2e44";
    ctx.fillRect(i * barWidth, (height - h) / 2, barWidth - 1, h);
  }

  $("time-current").textContent = formatTime(t);
  canvas.setAttribute("aria-valuenow", Math.round(t));
  canvas.setAttribute("aria-valuetext", formatTime(t));
}

function tick() {
  const r = state.result;
  if (r && state.source === "original" && !audio.original.paused && getTime() >= r.duration) {
    pause();
    setTime(0);
  }
  draw();
  if (!active().paused) requestAnimationFrame(tick);
}

function seekFromPointer(event) {
  if (!state.result) return;
  const rect = $("wave").getBoundingClientRect();
  setTime(((event.clientX - rect.left) / rect.width) * state.result.duration);
}

function bindPlayer() {
  for (const el of Object.values(audio)) {
    el.addEventListener("play", () => {
      $("play").classList.add("playing");
      $("play").setAttribute("aria-label", "Pause");
      requestAnimationFrame(tick);
    });
    el.addEventListener("pause", () => {
      $("play").classList.remove("playing");
      $("play").setAttribute("aria-label", "Play");
      draw();
    });
    el.addEventListener("ended", () => setTime(0));
    el.addEventListener("error", () => {
      if (el.getAttribute("src")) setStatus("Audio could not be loaded — render again.", "error");
    });
  }
  $("play").addEventListener("click", () => (active().paused ? play() : pause()));
  for (const b of document.querySelectorAll(".ab button")) {
    b.addEventListener("click", () => switchSource(b.dataset.source));
  }
  const wave = $("wave");
  wave.addEventListener("pointerdown", (event) => {
    wave.setPointerCapture(event.pointerId);
    seekFromPointer(event);
  });
  wave.addEventListener("pointermove", (event) => {
    if (wave.hasPointerCapture(event.pointerId)) seekFromPointer(event);
  });
  wave.addEventListener("keydown", (event) => {
    const delta = { ArrowLeft: -5, ArrowRight: 5 }[event.key];
    if (delta) {
      event.preventDefault();
      setTime(getTime() + delta);
    }
  });
  new ResizeObserver(draw).observe(wave);
}

/* ---------- boot ---------- */

$("search-form").addEventListener("submit", (event) => {
  event.preventDefault();
  const query = $("search-input").value.trim();
  if (query) search(query);
});
$("render").addEventListener("click", render);
$("advanced").addEventListener("change", (event) => {
  document.body.classList.toggle("show-advanced", event.target.checked);
});
for (const b of document.querySelectorAll(".render-controls .segmented button")) {
  b.addEventListener("click", () => {
    state.preview = b.dataset.preview === "1";
    for (const other of b.parentElement.children) other.setAttribute("aria-checked", other === b);
    settingsChanged();
  });
}
bindPlayer();
api("/api/config").then(buildSettings).catch((err) => setStatus(err.message, "error"));
