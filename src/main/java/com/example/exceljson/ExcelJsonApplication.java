package com.example.exceljson;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX launcher that delegates to {@link AppController} when the desktop UI is requested.
 */
public class ExcelJsonApplication extends Application {

    @Override
    public void start(Stage stage) {
        new AppController().start(stage);
    }
}
