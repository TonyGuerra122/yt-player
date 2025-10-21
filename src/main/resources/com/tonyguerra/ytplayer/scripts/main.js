// ---------- Utils ----------
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

function formatDuration(seconds) {
  if (!seconds || isNaN(seconds)) return "N/A";
  const min = Math.floor(seconds / 60);
  const sec = Math.floor(seconds % 60).toString().padStart(2, "0");
  return `${min}:${sec}`;
}

function parseStartTimeFromUrl(url) {
  try {
    const u = new URL(url);
    // ?t=337s ou ?start=337
    const t = u.searchParams.get("t") || u.searchParams.get("start");
    if (!t) return 0;
    // aceita "337s", "5m37s", "337"
    const match = /^((\d+)h)?((\d+)m)?((\d+)s)?$/.exec(t);
    if (match) {
      const h = parseInt(match[2] || 0, 10);
      const m = parseInt(match[4] || 0, 10);
      const s = parseInt(match[6] || 0, 10);
      return h * 3600 + m * 60 + s;
    }
    const n = parseInt(t, 10);
    return isNaN(n) ? 0 : n;
  } catch { return 0; }
}

// ---------- Loader ----------
const Loader = (() => {
  let overlay, text;
  function ensure() {
    overlay = document.getElementById("loadingOverlay");
    if (!overlay) {
      overlay = document.createElement("div");
      overlay.id = "loadingOverlay";
      overlay.style.cssText =
        "position:fixed;inset:0;display:none;align-items:center;justify-content:center;background:rgba(0,0,0,.35);z-index:9999";
      const box = document.createElement("div");
      box.style.cssText = "padding:16px 20px;border-radius:10px;background:#111;color:#fff;font:14px/1.3 system-ui;";
      text = document.createElement("div");
      text.textContent = "Carregando…";
      box.append(text);
      overlay.append(box);
      document.body.append(overlay);
    } else {
      text = overlay.querySelector("div div") || overlay.firstChild;
    }
  }
  return {
    show(msg = "Carregando…") {
      ensure();
      text.textContent = msg;
      overlay.style.display = "flex";
    },
    set(msg) {
      ensure();
      text.textContent = msg;
    },
    hide() { ensure(); overlay.style.display = "none"; }
  };
})();

// ---------- Bridge waiters ----------
function waitForJavaConnector(maxWaitMs = 8000) {
  const start = performance.now();
  return new Promise((resolve, reject) => {
    function tick() {
      if (window.javaConnector) return resolve(window.javaConnector);
      if (performance.now() - start > maxWaitMs) return reject(new Error("javaConnector indisponível"));
      requestAnimationFrame(tick);
    }
    tick();
  });
}

function waitForVideoInfo(maxWaitMs = 7000, signal) {
  const start = performance.now();
  return new Promise((resolve, reject) => {
    const id = setInterval(() => {
      if (signal?.aborted) {
        clearInterval(id);
        return reject(new DOMException("Abortado", "AbortError"));
      }
      try {
        const json = window.javaConnector.getVideoInfo();
        if (json) {
          clearInterval(id);
          resolve(json);
        } else if (performance.now() - start > maxWaitMs) {
          clearInterval(id);
          reject(new Error("Tempo esgotado ao obter dados do vídeo"));
        }
      } catch (e) {
        clearInterval(id);
        reject(e);
      }
    }, 120);
  });
}

// ---------- Core ----------
async function fetchVideoData(url, signal) {
  // dispara busca (não bloqueia a UI)
  window.javaConnector.searchVideo(url);

  // aguarda info
  Loader.set("Buscando metadados…");
  const json = await waitForVideoInfo(7000, signal);
  const data = JSON.parse(json);

  // tenta resolver streaming direto (prioriza API assíncrona, cai pra síncrona se faltar)
  let videoUrl = data.url;
  const haveAsync = typeof window.javaConnector.getVideoStreamingAsync === "function";

  if (haveAsync) {
    Loader.set("Gerando URL de streaming…");
    const streamUrl = await new Promise((resolve) => {
      // define callbacks temporários
      const prev = {
        onOk: window.onStreamReady,
        onErr: window.onStreamError
      };
      window.onStreamReady = (val) => {
        // cleanup
        window.onStreamReady = prev.onOk;
        window.onStreamError = prev.onErr;
        resolve(val || "");
      };
      window.onStreamError = () => {
        window.onStreamReady = prev.onOk;
        window.onStreamError = prev.onErr;
        resolve("");
      };
      // dispara
      window.javaConnector.getVideoStreamingAsync(url);
      // safety timeout
      setTimeout(() => {
        if (window.onStreamReady === prev.onOk) return; // já limpou
        window.onStreamReady = prev.onOk;
        window.onStreamError = prev.onErr;
        resolve("");
      }, 6000);
    });
    if (streamUrl && streamUrl.trim()) videoUrl = streamUrl.trim();
  } else if (typeof window.javaConnector.getVideoStreaming === "function") {
    Loader.set("Gerando URL de streaming…");
    try {
      const resolved = window.javaConnector.getVideoStreaming(url);
      if (resolved && resolved.trim()) videoUrl = resolved.trim();
    } catch { /* ignora e usa data.url */ }
  }

  // aplica início (t=…)
  const startSec = parseStartTimeFromUrl(url);
  if (startSec > 0) {
    // para <video>, podemos setar currentTime depois de carregar
    data._start = startSec;
  }

  return {
    title: data.title,
    duration: formatDuration(data.duration),
    author: data.author,
    thumbnail: data.thumbnail,
    video: videoUrl,
    start: data._start || 0
  };
}

// ---------- UI ----------
async function searchVideo() {
  const urlInput = document.getElementById("videoUrl");
  const preview = document.getElementById("videoPreview");
  const btn = document.getElementById("searchBtn");
  if (!urlInput || !preview) return;

  const url = urlInput.value.trim();
  if (!url) return;

  // controle de cancelamento para buscas consecutivas
  if (searchVideo._abort) searchVideo._abort.abort();
  const ctrl = new AbortController();
  searchVideo._abort = ctrl;

  try {
    btn && (btn.disabled = true);
    Loader.show("Validando URL…");
    preview.innerHTML = "";

    const result = await fetchVideoData(url, ctrl.signal);

    Loader.set("Renderizando…");
    preview.innerHTML = `
      <img src="${result.thumbnail}" alt="Thumb" style="max-width:100%;border-radius:10px;">
      <div class="video-info" style="margin:10px 0 6px">
        <p><strong>Título:</strong> ${result.title}</p>
        <p><strong>Duração:</strong> ${result.duration}</p>
        <p><strong>Autor:</strong> ${result.author}</p>
      </div>
      <video id="videoPlayer" controls style="width:100%;margin-top:8px;">
        <source src="${result.video}" type="video/mp4">
      </video>
    `;

    renderDownloadActions(preview, url);

    // aplica início (se houver)
    const player = document.getElementById("videoPlayer");
    if (player && result.start > 0) {
      // aguarda o metadata pra poder pular
      player.addEventListener("loadedmetadata", () => {
        try { player.currentTime = result.start; } catch {}
      }, { once: true });
    }
  } catch (err) {
    if (err?.name === "AbortError") return; // busca anterior cancelada
    console.error(err);
    preview.innerHTML = `<p style="color:#e33">Erro ao carregar o vídeo.<br>${String(err)}</p>`;
  } finally {
    Loader.hide();
    btn && (btn.disabled = false);
  }
}

// debounce na digitação do usuário
function debounce(fn, ms = 400) {
  let t;
  return (...args) => {
    clearTimeout(t);
    t = setTimeout(() => fn(...args), ms);
  };
}

function checkYtDlpAvailable() {
  return (
    window.javaConnector &&
    typeof javaConnector.isYtdlAvailable === "function" &&
    javaConnector.isYtdlAvailable()
  );
}

// ---------- Boot ----------
window.addEventListener("DOMContentLoaded", async () => {
  const btn = document.getElementById("searchBtn");
  const urlInput = document.getElementById("videoUrl");

  // F5 dentro do WebView
  window.addEventListener("keydown", (e) => {
    if (e.key === "F5") { e.preventDefault(); location.reload(); }
  });

  try {
    await waitForJavaConnector();
    console.log("javaConnector OK");

    // trava botão se yt-dlp indisponível (sem alert bloquante)
    if (btn) {
      const ok = checkYtDlpAvailable();
      btn.disabled = !ok;
      if (!ok) {
        const note = document.getElementById("note") || document.createElement("div");
        note.id = "note";
        note.textContent = "yt-dlp não encontrado. Instale para habilitar a busca.";
        note.style.cssText = "margin:8px 0;color:#e6b800;";
        btn.insertAdjacentElement("afterend", note);
      }
      btn.addEventListener("click", searchVideo);
    }

    // pesquisa automática com debounce ao digitar/colar
    if (urlInput) {
      const debounced = debounce(() => {
        if (!btn || !btn.disabled) searchVideo();
      }, 500);
      urlInput.addEventListener("input", debounced);
      urlInput.addEventListener("paste", debounced);
      urlInput.addEventListener("change", debounced);
    }
  } catch (e) {
    console.error(e);
    Loader.hide();
  }
});

// callbacks chamados pelo Java (ver executeScript acima)
window.onDownloadProgress = function (msg) {
  // reuse seu overlay
  if (msg) Loader.show(String(msg));
};

window.onDownloadOk = function (kind) {
  Loader.hide();
  toast(`Download de ${kind === 'audio' ? 'áudio' : 'vídeo'} concluído!`);
};

window.onDownloadError = function (kind, err) {
  Loader.hide();
  toast(`Falha no download de ${kind}: ${err || 'erro desconhecido'}`, true);
};

// utilitário simples de "toast" sem bloquear
function toast(text, error = false) {
  const t = document.createElement('div');
  t.textContent = text;
  t.style.cssText = `
    position: fixed; bottom: 18px; left: 50%; transform: translateX(-50%);
    background: ${error ? '#b00020' : '#222'};
    color: #fff; padding: 10px 14px; border-radius: 10px;
    box-shadow: 0 4px 18px rgba(0,0,0,.25); z-index: 99999; font: 13px system-ui;
  `;
  document.body.appendChild(t);
  setTimeout(() => { t.style.opacity = '0'; t.style.transition = 'opacity .3s'; }, 1800);
  setTimeout(() => t.remove(), 2100);
}

function renderDownloadActions(container, url) {
  if (!container) return;

  const canDownload =
    window.javaConnector &&
    typeof window.javaConnector.downloadVideo === "function" &&
    typeof window.javaConnector.downloadAudio === "function" &&
    typeof window.javaConnector.isYtdlAvailable === "function" &&
    window.javaConnector.isYtdlAvailable();

  const actions = document.createElement("div");
  actions.style.cssText = "margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;";

  const mkBtn = (label, bg) => {
    const b = document.createElement("button");
    b.textContent = label;
    b.style.cssText =
      `padding:8px 12px;border-radius:8px;border:none;background:${bg};color:#fff;cursor:pointer;opacity:${canDownload?1:.6}`;
    b.disabled = !canDownload;
    return b;
  };

  const btnVideo = mkBtn("⬇ baixar vídeo", "#2b7dfa");
  const btnAudio = mkBtn("⬇ baixar áudio", "#24a148");

  btnVideo.addEventListener("click", () => {
    if (!url) return toast("URL inválida", true);
    Loader.show("Preparando download de vídeo…");
    window.javaConnector.downloadVideo(url);
  });

  btnAudio.addEventListener("click", () => {
    if (!url) return toast("URL inválida", true);
    Loader.show("Preparando download de áudio…");
    window.javaConnector.downloadAudio(url);
  });

  actions.append(btnVideo, btnAudio);
  container.append(actions);

  if (!canDownload) {
    const note = document.createElement("div");
    note.textContent = "yt-dlp não encontrado ou métodos não expostos.";
    note.style.cssText = "font:12px system-ui;color:#e6b800;margin-top:6px;";
    container.append(note);
  }
}
