package com.jtune.app;

import com.jtune.database.DatabaseInitializer;
import com.jtune.service.MusicScannerService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class JTuneApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DatabaseInitializer initializer = new DatabaseInitializer();
        initializer.initialize();

        MusicScannerService scannerService = new MusicScannerService();
        scannerService.scanAndSyncLibrary();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        BorderPane root = loader.load();
        Scene scene = new Scene(root, 1020, 660);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());

        stage.setTitle("JTune");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(580);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
