module com.tonyguerra.ytplayer {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    opens com.tonyguerra.ytplayer.controllers to javafx.fxml;
    opens com.tonyguerra.ytplayer.core to javafx.fxml, javafx.graphics;

    exports com.tonyguerra.ytplayer;
}
