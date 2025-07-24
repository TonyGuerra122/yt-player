package com.tonyguerra.ytplayer.core;

import java.io.IOException;

import com.tonyguerra.ytplayer.utils.GUIUtils;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class AppGUI extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        GUIUtils.initializeScene(stage, "primary", 640, 480);
    }

    public static void main(String[] args) {
        launch();
    }

}