package com.tonyguerra.ytplayer.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tonyguerra.ytplayer.App;
import com.tonyguerra.ytplayer.constants.Mappers;
import com.tonyguerra.ytplayer.data.VideoInfo;
import com.tonyguerra.ytplayer.utils.YtUtils;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public final class PrimaryController implements Initializable {
  @FXML
  private WebView webView;

  private JavaBridge javaBridge; // <-- adicionado

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
      System.out.println("JavaScript Alert: " + event.getMessage());
    });

    engine.setOnAlert(event -> {
      System.out.println("JavaScript Alert: " + event.getData());
    });

    engine.documentProperty().addListener((obs, oldDoc, newDoc) -> {
      if (newDoc != null) {
        javaBridge = new JavaBridge(); // <-- agora armazenamos
        final var window = (JSObject) engine.executeScript("window");
        window.setMember("javaConnector", javaBridge);
      }
    });

    engine.loadContent(html);
  }

  public class JavaBridge {
    private VideoInfo videoInfo;

    public JavaBridge() {
      videoInfo = null;
    }

    public void searchVideo(String url) {
      if (url == null || url.isEmpty()) {
        return;
      }

      try {
        final var video = YtUtils.searchVideo(url);

        if (!video.isPresent()) {
          return;
        }
        videoInfo = video.get();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    public String getVideoInfo() {
      if (videoInfo == null) {
        return null;
      }

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
  }
}
