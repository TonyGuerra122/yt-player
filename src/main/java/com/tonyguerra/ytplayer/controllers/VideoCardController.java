package com.tonyguerra.ytplayer.controllers;

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
            YtUtils.downloadVideo(video, "video");
            Platform.runLater(() -> {
                Toast.showToast(videoCard, "Download complete");
            });
        }).start();
    }
}
