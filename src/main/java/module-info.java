module com.tonyguerra.ytplayer {
    requires javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires com.tonyguerra.ytdownloader;
    requires java.xml;
    requires java.net.http;

    exports com.tonyguerra.ytplayer;

    opens com.tonyguerra.ytplayer.controllers to javafx.fxml;
}
