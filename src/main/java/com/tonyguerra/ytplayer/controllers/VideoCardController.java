package com.tonyguerra.ytplayer.controllers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import com.tonyguerra.ytdownloader.dto.Video;
import com.tonyguerra.ytdownloader.utils.YtUtils;
import com.tonyguerra.ytplayer.components.Toast;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public final class VideoCardController {
    private VBox videoCard;

    @FXML
    private ImageView thumbnailView;

    @FXML
    private Label titleLabel;

    @FXML
    private Label metaLabel;

    private Video video;

    public void setData(VBox box, Video video) {
        this.video = video;
        this.videoCard = box;

        final String originalUrl = video.thumbnail();
        final String imageUrl = originalUrl.replace("vi_webp", "vi").replace(".webp", ".jpg");

        System.out.println("Corrigido URL da imagem: " + imageUrl);

        final var image = new Image(imageUrl, true);
        image.errorProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                System.err.println("Erro ao carregar imagem corrigida: " + imageUrl);
            }
        });

        thumbnailView.setImage(image);
        titleLabel.setText(video.title());
        metaLabel.setText(String.format("%s · %.2f seg", video.author(), video.duration()));
    }

    @FXML
    private void onDownloadAction(ActionEvent event) {
        Platform.runLater(() -> Toast.showToast(videoCard, "Downloading " + video.title()));
        new Thread(() -> {
            final String videoFolder = Path.of(System.getProperty("user.home"), "Videos", "YTPlayer").toString()
                    + File.separator;

            new File(videoFolder).mkdirs();

            YtUtils.downloadVideo(video, videoFolder);
            Platform.runLater(() -> {
                Toast.showToast(videoCard, "Download complete");
            });

            final String os = System.getProperty("os.name").toLowerCase();

            try {
                final String title = video.title().replaceAll("[^a-zA-Z0-9\\s]", "_");

                final String filePath = testAndGetExtFile(videoFolder + title);

                if (os.contains("win")) {
                    new ProcessBuilder("cmd", "/c", "start", "", filePath).start();
                } else if (os.contains("nix")) {
                    new ProcessBuilder("xdg-open", filePath).start();
                } else {
                    System.err.println("Unsupported OS: " + os);
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Toast.showToast(videoCard, "Error opening video file");
                });

                ex.printStackTrace();
            }
        }).start();
    }

    private static String testAndGetExtFile(String file) {
        if (Files.exists(Path.of(file + ".webm"))) {
            return file + ".webm";
        } else if (Files.exists(Path.of(file + ".mp4"))) {
            return file + ".mp4";
        } else if (Files.exists(Path.of(file + ".mkv"))) {
            return file + ".mkv";
        } else if (Files.exists(Path.of(file + ".flv"))) {
            return file + ".flv";
        } else if (Files.exists(Path.of(file + ".avi"))) {
            return file + ".avi";
        } else if (Files.exists(Path.of(file + ".mov"))) {
            return file + ".mov";
        } else if (Files.exists(Path.of(file + ".wmv"))) {
            return file + ".wmv";
        } else {
            return null;
        }
    }
}
