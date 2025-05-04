package com.tonyguerra.ytplayer.controllers;

import com.tonyguerra.ytdownloader.utils.YtUtils;
import com.tonyguerra.ytplayer.App;
import com.tonyguerra.ytplayer.components.Toast;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

public final class PrimaryController {
    @FXML
    private BorderPane rootPane;

    @FXML
    private VBox contentBox;

    @FXML
    private TextField urlField;

    @FXML
    private VBox resultsBox;

    @FXML
    private void onSearchAction(ActionEvent event) {
        final String url = urlField.getText();

        if (url.isEmpty())
            return;

        Platform.runLater(() -> Toast.showToast(contentBox, "Searching for " + url));

        new Thread(() -> {
            try {
                final var video = YtUtils.getVideo(url);
                final FXMLLoader loader = App.loadFXMLLoader("video-card");
                final Node cardNode = loader.load();

                final VideoCardController controller = loader.getController();
                controller.setData(resultsBox, video);

                Platform.runLater(() -> {
                    resultsBox.getChildren().clear();
                    resultsBox.getChildren().add(cardNode);
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> Toast.showToast(contentBox, "Error: " + ex.getMessage()));
            }
        }).start();
    }

}
