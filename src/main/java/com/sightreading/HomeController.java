package com.sightreading;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextInputDialog;
import java.io.IOException;
import java.util.Optional;

public class HomeController {

    @FXML
    private void handlePlay(ActionEvent event) {
        if (Main.playerName == null || Main.playerName.trim().isEmpty()) {
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("Player Login");
            dialog.setHeaderText("Welcome to the Rhythm Game!");
            dialog.setContentText("Enter your name for the leaderboard:");

            Optional<String> result = dialog.showAndWait();
            
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                Main.playerName = result.get().trim();
                System.out.println("Player logged in as: " + Main.playerName);
                goToSongList();
            } else {
                System.out.println("Login cancelled or empty name.");
            }
        } else {
            System.out.println("Welcome back, " + Main.playerName);
            goToSongList();
        }
    }

    private void goToSongList() {
        try {
            Main.setRoot("song-list");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLeaderboard(ActionEvent event) {
        System.out.println("Leaderboard button clicked.");
    }
}