package com.tonyguerra.ytplayer.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tonyguerra.ytplayer.App;
import com.tonyguerra.ytplayer.constants.Mappers;
import com.tonyguerra.ytplayer.data.VideoInfo;
import com.tonyguerra.ytplayer.enums.DownloadType;
import com.tonyguerra.ytplayer.utils.YtUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public final class PrimaryController implements Initializable {
  @FXML
  private WebView webView;

  // ⇩⇩⇩ ADD: executor para rodar tasks sem travar a UI
  private final ExecutorService executor = Executors.newFixedThreadPool(
      Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

  private JavaBridge javaBridge;

  @Override
  @FXML
  public void initialize(URL location, ResourceBundle resources) {
    final String html;
    try {
      html = App.loadHTML("main");
    } catch (IOException ex) {
      throw new RuntimeException("Error loading HTML content", ex);
    }

    final var engine = webView.getEngine();
    engine.setJavaScriptEnabled(true);

    engine.setOnError(event -> {
      System.out.println("JavaScript Error: " + event.getMessage());
    });

    engine.setOnAlert(event -> {
      System.out.println("JavaScript Alert: " + event.getData());
    });

    engine.documentProperty().addListener((obs, oldDoc, newDoc) -> {
      if (newDoc != null) {
        javaBridge = new JavaBridge();
        final var window = (JSObject) engine.executeScript("window");
        window.setMember("javaConnector", javaBridge);
      }
    });

    engine.loadContent(html);
  }

  // boa prática: chame ao fechar a janela/app
  public void shutdown() {
    executor.shutdownNow();
  }

  public class JavaBridge {
    private VideoInfo videoInfo;

    public JavaBridge() {
      videoInfo = null;
    }

    public void searchVideo(String url) {
      if (url == null || url.isEmpty()) return;

      try {
        final var video = YtUtils.searchVideo(url);
        if (video.isPresent()) {
          videoInfo = video.get();
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    public String getVideoInfo() {
      if (videoInfo == null) return null;
      try {
        return Mappers.JSON_MAPPER.writeValueAsString(videoInfo);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
        return null;
      }
    }

    public String getVideoStreaming(String url) {
      return YtUtils.getVideoStreamingUrl(url);
    }

    public boolean isYtdlAvailable() {
      return YtUtils.isYtDlpPresent();
    }

    // ⇩⇩⇩ ADD: expõe para o JS baixar VÍDEO
    public void downloadVideo(String url) {
      runDownloadTask(url, DownloadType.VIDEO, "Baixando vídeo…");
    }

    // ⇩⇩⇩ ADD: expõe para o JS baixar ÁUDIO
    public void downloadAudio(String url) {
      runDownloadTask(url, DownloadType.AUDIO, "Baixando áudio…");
    }

    private void runDownloadTask(String url, DownloadType type, String label) {
      if (url == null || url.isBlank()) return;

      final var task = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
          YtUtils.ensureYtDlpInstalled();
          YtUtils.downloadVideo(url, type);
          return null;
        }
      };

      task.setOnScheduled(e -> Platform.runLater(() -> {
        try {
          webView.getEngine().executeScript("""
            (function(msg){
              if (window.onDownloadProgress) window.onDownloadProgress(msg);
            })
          """ + "('" + jsEscape(label) + "');");
        } catch (Exception ignored) {}
      }));

      task.setOnSucceeded(e -> Platform.runLater(() -> {
        try {
          webView.getEngine().executeScript("""
            (function(kind){
              if (window.onDownloadOk) window.onDownloadOk(kind);
            })
          """ + "('" + (type == DownloadType.AUDIO ? "audio" : "video") + "');");
        } catch (Exception ignored) {}
      }));

      task.setOnFailed(e -> Platform.runLater(() -> {
        final String kind = (type == DownloadType.AUDIO ? "audio" : "video");
        final String msg  = task.getException() != null ? task.getException().getMessage() : "Erro desconhecido";
        try {
          webView.getEngine().executeScript("""
            (function(kind, msg){
              if (window.onDownloadError) window.onDownloadError(kind, msg);
            })
          """ + "('" + jsEscape(kind) + "','" + jsEscape(msg) + "');");
        } catch (Exception ignored) {}
      }));

      // ⇩⇩⇩ ADD: roda de fato (sem isso a Task nunca executa)
      executor.submit(task);
    }

    private String jsEscape(String s) {
      if (s == null) return "";
      return s.replace("\\", "\\\\")
              .replace("'", "\\'")
              .replace("\n", "\\n")
              .replace("\r", "");
    }
  }
}
