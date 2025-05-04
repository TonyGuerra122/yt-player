module com.tonyguerra.ytplayer {
    requires javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires com.tonyguerra.ytdownloader;

    opens com.tonyguerra.ytplayer.controllers to javafx.fxml;

    exports com.tonyguerra.ytplayer;
}
