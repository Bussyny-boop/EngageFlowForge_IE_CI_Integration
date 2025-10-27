package com.example.exceljson;

import com.example.exceljson.jobs.JobRunner;
import javafx.application.Application;

public final class Main extends ExcelJsonApplication {

    public static void main(String[] args) {
        Integer jobStatus = maybeRunJob(args);
        if (jobStatus != null) {
            if (jobStatus != 0) {
                System.exit(jobStatus);
            }
            return;
        }

        // ✅ Launch JavaFX app using inherited Application context
        try {
            launch(args);
        } catch (NoClassDefFoundError error) {
            if (error.getMessage() != null && error.getMessage().startsWith("javafx/")) {
                System.err.println("Unable to start Engage Rules Generator because JavaFX runtime classes are missing.");
                System.err.println("If you are packaging the application, include the JavaFX modules, for example:");
                System.err.println("  jpackage --java-options \"--add-modules=javafx.controls,javafx.fxml\" ...");
                System.exit(1);
                return;
            }
            throw error;
        }
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
