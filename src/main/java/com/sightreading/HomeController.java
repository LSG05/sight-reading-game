package com.sightreading;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.io.IOException;

public class HomeController {

    @FXML private TextField nameField;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        if (Main.playerName != null && !Main.playerName.isEmpty()) {
            nameField.setText(Main.playerName);
        }
    }

    @FXML
    private void handlePlay(ActionEvent event) {
        String enteredName = nameField.getText().trim();

        if (enteredName.isEmpty()) {
            errorLabel.setText("Please enter a name to play!");
        } else {
            errorLabel.setText(""); 
            Main.playerName = enteredName;
            System.out.println("Player logged in as: " + Main.playerName);
            
            try {
                Main.setRoot("song-list");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleLeaderboard(ActionEvent event) {
        System.out.println("Leaderboard button clicked.");
    }
}