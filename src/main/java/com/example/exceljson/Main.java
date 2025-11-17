package com.example.exceljson;

import javafx.application.Application;

public final class Main {
    public static void main(String[] args) {
        try {
            Application.launch(ExcelJsonApplication.class, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
