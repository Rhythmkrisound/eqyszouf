const $ = (id) => document.getElementById(id);

// Preset names live in i18n.js as preset.<index>.
const PRESETS = [
  {},
  { speed: 0.92, semitones: -1.5, bass: 6, presence: 2, air: 3, drive: 1, reverb_mix: 0.2 },
  { speed: 0.8, semitones: -4, reverb_mix: 0.6, reverb_size: 0.95, reverb_damping: 0.2, reverb_tail: 4 },
  { speed: 1, semitones: 0, bass: 12, presence: 2, air: 2, drive: 2, reverb_mix: 0 },
  { speed: 1.25, semitones: 3.86, bass: 3, presence: 2, air: 3, drive: 0, reverb_mix: 0.08, reverb_size: 0.4 },
];

function initialLang() {
  let saved = null;
  try { saved = localStorage.getItem("lang"); } catch {}
  if (saved && I18N[saved]) return saved;
  return (navigator.language || "").toLowerCase().startsWith("ru") ? "ru" : "en";
}

const state = {
  lang: initialLang(),
  fields: {},      // key -> { def, range, number, row, label, unit }
  groups: [],      // [{ title, heading }]
  defaults: {},
  previewLength: 45,
  maxDuration: Infinity,
  tracks: null,    // last search results; null until a search succeeds
  hint: { key: "hint.start" },
  track: null,
  preview: true,
  result: null,    // finished job plus the speed it was rendered with
  renderedKey: null,
  source: "remix",
  requestId: 0,
  status: null,    // { key, params, kind } or { raw, kind }
};

const audio = { remix: $("audio-remix"), original: $("audio-original") };

/* ---------- language ---------- */

function t(key, params = {}, fallback = key) {
  const text = I18N[state.lang][key] ?? I18N.en[key] ?? fallback;
  return text.replace(/\{(\w+)\}/g, (_, name) => params[name]);
}

function translateError(message) {
  for (const [pattern, replacement] of ERRORS[state.lang] || []) {
    if (pattern.test(message)) return message.replace(pattern, replacement);
  }
  return message;
}

function applyLanguage() {
  document.documentElement.lang = state.lang;
  for (const b of $("lang").children) b.setAttribute("aria-checked", b.dataset.lang === state.lang);
  for (const el of document.querySelectorAll("[data-i18n]")) el.textContent = t(el.dataset.i18n);
  for (const el of document.querySelectorAll("[data-i18n-attr]")) {
    for (const pair of el.dataset.i18nAttr.split(",")) {
      const [attr, key] = pair.split(":");
      el.setAttribute(attr, t(key));
    }
  }
  $("mode-preview").textContent = t("mode.preview", { n: state.previewLength });
  for (const g of state.groups) g.heading.textContent = t(`group.${g.title}`, {}, g.title);
  for (const [key, f] of Object.entries(state.fields)) {
    const label = t(`field.${key}`, {}, f.def.label);
    f.label.textContent = label;
    f.unit.textContent = t(`unit.${f.def.unit}`, {}, f.def.unit);
    f.number.setAttribute("aria-label", t("field.value", { label }));
    f.range.title = t("field.reset");
  }
  for (const chip of $("presets").children) chip.textContent = t(`preset.${chip.dataset.index}`);
  for (const option of $("bitrate").options) option.textContent = `${option.value} ${t("kbps")}`;
  renderResults();
  renderNow();
  renderStatus();
  renderPlayerText();
}

function setLanguage(lang) {
  state.lang = lang;
  try { localStorage.setItem("lang", lang); } catch {}
  applyLanguage();
}

/* ---------- helpers ---------- */

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
    const target = { ...state.defaults, ...PRESETS[chip.dataset.index] };
    const match = Object.keys(target).every((k) => Math.abs(target[k] - settings[k]) < 1e-9);
    chip.setAttribute("aria-pressed", match);
  }
  const busy = state.status && state.status.kind === "busy";
  if (state.result && state.renderedKey !== requestKey() && !busy) setStatus("status.changed");
}

function buildSettings(config) {
  state.previewLength = config.previewLength;
  state.maxDuration = config.maxDuration;
  for (const group of config.groups) {
    const box = document.createElement("div");
    box.className = "group";
    const heading = document.createElement("h3");
    box.append(heading);
    state.groups.push({ title: group.title, heading });
    for (const def of group.fields) {
      state.defaults[def.key] = def.default;
      const row = document.createElement("div");
      row.className = "field" + (def.advanced ? " advanced" : "");
      const label = document.createElement("label");
      label.htmlFor = `f-${def.key}`;
      const value = document.createElement("div");
      value.className = "value";
      const number = document.createElement("input");
      number.type = "number";
      const unit = document.createElement("span");
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
      row.append(label, value, range);
      box.append(row);
      state.fields[def.key] = { def, range, number, row, label, unit };
      setField(def.key, def.default, { silent: true });
    }
    $("groups").append(box);
  }

  PRESETS.forEach((values, index) => {
    const chip = document.createElement("button");
    chip.type = "button";
    chip.className = "chip";
    chip.dataset.index = index;
    chip.addEventListener("click", () => {
      const target = { ...state.defaults, ...values };
      for (const key of Object.keys(target)) setField(key, target[key], { silent: true });
      settingsChanged();
    });
    $("presets").append(chip);
  });

  for (const rate of config.bitrates) {
    $("bitrate").append(new Option("", rate, false, rate === config.defaultBitrate));
  }
  $("bitrate").addEventListener("change", settingsChanged);
  applyLanguage();
  settingsChanged();
}

/* ---------- search ---------- */

function artStyle(el, url) {
  el.style.backgroundImage = url ? `url("${encodeURI(url)}")` : "";
}

function renderResults() {
  const list = $("results");
  if (list.querySelector(".skeleton")) return;
  if (!state.tracks || !state.tracks.length) {
    const p = document.createElement("p");
    p.className = "hint" + (state.hint.raw ? " error" : "");
    p.textContent = state.hint.raw ? translateError(state.hint.raw) : t(state.hint.key);
    list.replaceChildren(p);
    return;
  }
  list.replaceChildren();
  for (const track of state.tracks) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "track";
    button.setAttribute("aria-pressed", Boolean(state.track && state.track.url === track.url));
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
      button.title = t("track.tooLong", { n: Math.round(state.maxDuration / 60) });
    }
    button.addEventListener("click", () => selectTrack(track));
    list.append(button);
  }
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
    state.tracks = tracks;
    state.hint = { key: "hint.empty" };
    $("results").replaceChildren();
    if (tracks.length === 1) selectTrack(tracks[0]);
    else renderResults();
  } catch (err) {
    if (id !== searchId) return;
    state.tracks = null;
    state.hint = { raw: err.message };
    $("results").replaceChildren();
    renderResults();
  }
}

function renderNow() {
  const track = state.track;
  $("now-title").textContent = track ? track.title : t("now.none");
  $("now-artist").textContent = track
    ? [track.artist, track.duration && formatTime(track.duration)].filter(Boolean).join(" · ")
    : t("now.pick");
}

function selectTrack(track) {
  state.track = track;
  renderResults();
  renderNow();
  artStyle($("now-art"), track.artwork);
  $("render").disabled = false;
  setStatus("status.ready");
}

/* ---------- rendering ---------- */

function renderStatus() {
  const el = $("status");
  const s = state.status;
  el.textContent = !s ? "" : s.raw ? translateError(s.raw) : t(s.key, s.params);
  el.className = "status" + (s && s.kind ? ` ${s.kind}` : "");
}

function setStatus(key, kind = "", params = {}) {
  state.status = { key, kind, params };
  renderStatus();
}

function setError(message) {
  state.status = { raw: message, kind: "error" };
  renderStatus();
}

const JOB_STATUSES = ["queued", "downloading", "processing", "encoding"];

async function render() {
  if (!state.track) return;
  const id = ++state.requestId;
  const track = state.track;
  const settings = currentSettings();
  const key = requestKey();
  $("render").disabled = true;
  setStatus("status.queued", "busy");
  try {
    let job = await api("/api/remix", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url: track.url, settings, bitrate: Number($("bitrate").value), preview: state.preview }),
    });
    while (job.status !== "done" && job.status !== "error") {
      if (id !== state.requestId) return;
      setStatus(JOB_STATUSES.includes(job.status) ? `status.${job.status}` : "status.working", "busy");
      await new Promise((r) => setTimeout(r, 700));
      job = await api(`/api/jobs/${job.id}`);
    }
    if (id !== state.requestId) return;
    if (job.status === "error") throw new Error(job.error || "Render failed");
    state.renderedKey = key;
    loadResult({ ...job, speed: settings.speed, track });
    if (job.preview && job.sourceStart > 0) setStatus("status.previewReady", "", { t: formatTime(job.sourceStart) });
    else setStatus("status.done");
  } catch (err) {
    if (id === state.requestId) setError(err.message);
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
  $("time-total").textContent = formatTime(result.duration);
  for (const b of document.querySelectorAll(".ab button")) b.disabled = false;
  const link = $("download");
  link.classList.remove("disabled");
  link.removeAttribute("aria-disabled");
  $("wave").setAttribute("aria-valuemax", Math.round(result.duration));
  renderPlayerText();
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

function setTime(time) {
  const r = state.result;
  if (!r) return;
  time = Math.min(Math.max(time, 0), r.duration);
  if (state.source === "remix") audio.remix.currentTime = time;
  else audio.original.currentTime = r.sourceStart + Math.min(time * r.speed, r.sourceDuration);
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

function renderPlayerText() {
  const r = state.result;
  for (const b of document.querySelectorAll(".ab button")) {
    b.setAttribute("aria-checked", b.dataset.source === state.source);
  }
  $("play").setAttribute("aria-label", t(active().paused ? "player.play" : "player.pause"));
  if (!r) {
    $("player-title").textContent = t("player.none");
    $("player-sub").textContent = t("player.noneSub");
    return;
  }
  $("player-title").textContent = r.track.title;
  $("player-sub").textContent = state.source === "original"
    ? t("player.original")
    : t(r.preview ? "player.remixPreview" : "player.remix");
  const name = `${r.track.title} (${t("download.suffix")})`;
  $("download").href = `${r.audio}?download=${encodeURIComponent(name)}`;
}

function switchSource(source) {
  if (!state.result || source === state.source) return;
  const time = getTime();
  const wasPlaying = !active().paused;
  pause();
  state.source = source;
  setTime(time);
  renderPlayerText();
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
  const time = getTime();
  const progress = r ? Math.min(1, time / r.duration) : 0;
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

  $("time-current").textContent = formatTime(time);
  canvas.setAttribute("aria-valuenow", Math.round(time));
  canvas.setAttribute("aria-valuetext", formatTime(time));
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
      renderPlayerText();
      requestAnimationFrame(tick);
    });
    el.addEventListener("pause", () => {
      $("play").classList.remove("playing");
      renderPlayerText();
      draw();
    });
    el.addEventListener("ended", () => setTime(0));
    el.addEventListener("error", () => {
      if (el.getAttribute("src")) setError("Audio could not be loaded — render again.");
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
for (const b of $("lang").children) b.addEventListener("click", () => setLanguage(b.dataset.lang));
$("upload-button").addEventListener("click", () => $("upload").click());
$("upload").addEventListener("change", async (event) => {
  const file = event.target.files[0];
  event.target.value = "";
  if (!file) return;
  setStatus("status.uploading", "busy");
  try {
    selectTrack(await api(`/api/upload?name=${encodeURIComponent(file.name)}`, { method: "POST", body: file }));
  } catch (err) {
    setError(err.message);
  }
});
bindPlayer();
applyLanguage();
api("/api/config").then(buildSettings).catch((err) => setError(err.message));
