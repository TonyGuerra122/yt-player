package com.tonyguerra.ytplayer.utils;

import java.io.IOException;

import com.tonyguerra.ytplayer.Main;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class GUIUtils {
    private static Scene scene = null;

    public static void initializeScene(Stage stage, String fxml, int width, int height) throws IOException {
        scene = new Scene(loadFXML(fxml), width, height);

        stage.setScene(scene);
        stage.show();
    }

    private static Parent loadFXML(String fxml) throws IOException {
        final var fxmlLoader = new FXMLLoader(Main.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void setRoot(String fxml) throws IOException {
        if (scene == null) {
            scene = new Scene(loadFXML(fxml));
        } else {
            scene.setRoot(loadFXML(fxml));
        }
    }

}
