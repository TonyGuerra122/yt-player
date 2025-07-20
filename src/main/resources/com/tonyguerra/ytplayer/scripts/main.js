function waitForJavaConnector(callback) {
  if (window.javaConnector) return callback();

  const interval = setInterval(() => {
    if (window.javaConnector) {
      clearInterval(interval);
      callback();
    }
  }, 100);
}

function formatDuration(seconds) {
  if (!seconds || isNaN(seconds)) return "N/A";
  const min = Math.floor(seconds / 60);
  const sec = Math.floor(seconds % 60)
    .toString()
    .padStart(2, "0");
  return `${min}:${sec}`;
}

function waitForVideoInfo(maxWaitMs = 5000) {
  const start = Date.now();

  return new Promise((resolve, reject) => {
    const interval = setInterval(() => {
      const json = javaConnector.getVideoInfo();
      if (json) {
        clearInterval(interval);
        resolve(json);
      } else if (Date.now() - start > maxWaitMs) {
        clearInterval(interval);
        reject("Tempo esgotado para obter dados do vídeo.");
      }
    }, 100);
  });
}

async function fetchVideoData(url) {
  try {
    javaConnector.searchVideo(url);

    const json = await waitForVideoInfo();
    const data = JSON.parse(json);

    let videoUrl = data.url;

    try {
      const resolved = javaConnector.getVideoStreaming(url);
      if (resolved && resolved.trim()) {
        videoUrl = resolved;
        alert("Streaming direto: " + resolved);
      } else {
        alert("getVideoStreaming retornou vazio");
      }
    } catch (err) {
      alert("Erro ao obter streaming direto: " + err);
    }

    return {
      title: data.title,
      duration: formatDuration(data.duration),
      author: data.author,
      thumbnail: data.thumbnail,
      video: videoUrl,
    };
  } catch (err) {
    throw new Error("Erro ao buscar dados do vídeo: " + err);
  }
}

async function searchVideo() {
  const urlInput = document.getElementById("videoUrl");
  const preview = document.getElementById("videoPreview");

  if (!urlInput || !preview) return;

  const url = urlInput.value.trim();
  if (!url) return;

  preview.innerHTML = "<p>Carregando...</p>";

  try {
    const result = await fetchVideoData(url);

    preview.innerHTML = `
      <img src="${result.thumbnail}" alt="Thumb" style="max-width: 100%; border-radius: 10px;">
      <div class="video-info">
        <p><strong>Título:</strong> ${result.title}</p>
        <p><strong>Duração:</strong> ${result.duration}</p>
        <p><strong>Autor:</strong> ${result.author}</p>
      </div>
      <video id="videoPlayer" controls style="width: 100%; margin-top: 20px;">
        <source src="${result.video}" type="video/mp4">
      </video>
    `;
  } catch (err) {
    console.error(err);
    preview.innerHTML = `<p style='color:red;'>Erro ao carregar o vídeo.<br> ${err}</p>`;
  }
}

function checkYtDlpAvailable() {
  return (
    window.javaConnector &&
    typeof javaConnector.isYtdlAvailable === "function" &&
    javaConnector.isYtdlAvailable()
  );
}

window.addEventListener("DOMContentLoaded", () => {
  const btn = document.getElementById("searchBtn");
  const overlay = document.getElementById("loadingOverlay");

  window.addEventListener("keydown", (e) => {
    if (e.key === "F5") {
      e.preventDefault();
      location.reload();
    }
  });

  waitForJavaConnector(() => {
    console.log("javaConnector disponível:", javaConnector);

    if (btn) {
      const ytAvailable = checkYtDlpAvailable();
      btn.disabled = !ytAvailable;

      if (!ytAvailable) {
        alert(
          "yt-dlp não está instalado. Por favor, instale antes de continuar."
        );
        return;
      }

      btn.addEventListener("click", async () => {
        if (!overlay) return;
        overlay.style.display = "flex";

        try {
          await searchVideo();
        } finally {
          overlay.style.display = "none";
        }
      });
    }
  });
});
