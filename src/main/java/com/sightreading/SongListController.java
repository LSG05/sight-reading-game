package com.sightreading;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import java.io.IOException;

public class SongListController {

    @FXML
    private void handlePlayTwinkle(ActionEvent event) {
        System.out.println("Twinkle Twinkle Little Star selected. Starting game...");
        try {
            Main.setRoot("game");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        System.out.println("Returning to Home Menu...");
        try {
            Main.setRoot("home");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}