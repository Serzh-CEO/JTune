module com.jtune {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires mp3agic;

    opens com.jtune.controller to javafx.fxml;
    exports com.jtune.app;
    exports com.jtune.model;
}
