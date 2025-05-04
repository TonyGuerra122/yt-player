package com.tonyguerra.ytplayer.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public final class Toast {
    public static void showToast(Pane root, String message) {
        final var toast = new Label(message);
        toast.getStyleClass().addAll("toast");
        toast.setOpacity(0);

        toast.setLayoutX((root.getWidth() - 300) / 2);
        toast.setLayoutY(root.getHeight() - 80);

        root.getChildren().add(toast);

        final var fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        final var pause = new PauseTransition(Duration.seconds(2));

        final var fadeOut = new FadeTransition(Duration.millis(300), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        fadeOut.setOnFinished(e -> root.getChildren().remove(toast));

        final var seq = new SequentialTransition(fadeIn, pause, fadeOut);

        seq.play();
    }
}
