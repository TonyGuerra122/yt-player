module com.tonyguerra.ytplayer {
    requires javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires java.xml;
    requires java.net.http;
    requires javafx.web;
    requires javafx.media;
    requires jdk.jsobject;
    requires com.fasterxml.jackson.databind;

    exports com.tonyguerra.ytplayer;
    exports com.tonyguerra.ytplayer.controllers;
    exports com.tonyguerra.ytplayer.data to com.fasterxml.jackson.databind;
    exports com.tonyguerra.ytplayer.enums to com.fasterxml.jackson.databind;

    opens com.tonyguerra.ytplayer.controllers to javafx.fxml, javafx.web;
}
