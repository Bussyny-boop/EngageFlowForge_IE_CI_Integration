package com.example.exceljson;

import com.example.exceljson.jobs.JobRunner;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        new AppController().start(stage);
    }

    public static void main(String[] args) {
        Integer jobStatus = maybeRunJob(args);
        if (jobStatus != null) {
            if (jobStatus != 0) {
                System.exit(jobStatus);
            }
            return;
        }
        launch(args);
    }

    static Integer maybeRunJob(String[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        JobRunner runner = new JobRunner();
        if (!runner.isKnownJob(args[0])) {
            return null;
        }

        return runner.run(args);
    }
}
