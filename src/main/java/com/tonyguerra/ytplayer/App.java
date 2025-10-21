package com.tonyguerra.ytplayer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.tonyguerra.ytplayer.utils.YtUtils;

/**
 * JavaFX App
 */
public final class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        stage.setTitle("YT Player");
        stage.getIcons().add(new Image(App.class.getResourceAsStream("/com/tonyguerra/ytplayer/favicon.ico")));

        // Exibe uma tela temporária de carregamento
        stage.setScene(new Scene(loadFXML("splash"), 400, 200));
        stage.show();

        // Verifica e instala o yt-dlp em uma thread separada
        new Thread(() -> {
            try {
                Platform.runLater(() -> System.out.println("Verificando yt-dlp..."));
                YtUtils.ensureYtDlpInstalled();

                // Após verificação, carrega a tela principal
                Platform.runLater(() -> {
                    try {
                        scene = new Scene(loadFXML("primary"), 700, 700);
                        stage.setScene(scene);
                        stage.centerOnScreen();
                    } catch (IOException e) {
                        showFatalError(stage, "Erro ao carregar a interface principal.", e);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showFatalError(stage, "Erro ao verificar yt-dlp", e));
            }
        }, "YTDLP Initializer").start();
    }

    private void showFatalError(Stage stage, String message, Exception e) {
        e.printStackTrace();
        var errorBox = new VBox(
                new Label(message),
                new Label(e.getMessage()));
        errorBox.setSpacing(10);
        errorBox.setPadding(new Insets(20));
        var scene = new Scene(errorBox, 500, 200);
        stage.setScene(scene);
    }

    public static Parent loadFXML(String fxml) throws IOException {
        final var fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static String loadHTML(String html) throws IOException {
        var htmlResource = App.class.getResource("pages/" + html + ".html");
        if (htmlResource == null) {
            throw new RuntimeException("HTML não encontrado: pages/" + html + ".html");
        }

        String htmlContent;
        try (var stream = htmlResource.openStream()) {
            htmlContent = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Carrega o CSS embutido
        var cssResource = App.class.getResource("styles/main.css");
        String cssContent = "";
        if (cssResource != null) {
            try (var stream = cssResource.openStream()) {
                cssContent = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        // Carrega o JS embutido
        var jsResource = App.class.getResource("scripts/main.js");
        String jsContent = "";
        if (jsResource != null) {
            try (var stream = jsResource.openStream()) {
                jsContent = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        // Substitui no HTML
        htmlContent = htmlContent
                .replace("<!-- {{styles}} -->", "<style>\n" + cssContent + "\n</style>")
                .replace("<!-- {{scripts}} -->", "<script>\n" + jsContent + "\n</script>");

        return htmlContent;
    }

    public static FXMLLoader loadFXMLLoader(String fxml) throws IOException {
        return new FXMLLoader(App.class.getResource(fxml + ".fxml"));
    }

    public static void main(String[] args) {
        launch();
    }

}