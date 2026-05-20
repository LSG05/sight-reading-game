package com.sightreading;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import java.io.IOException;
import java.net.URL;

public class HomeController {

    @FXML private TextField nameField;
    @FXML private Label errorLabel;
    @FXML private MediaView bgMediaView; // from home.FXML
    private MediaPlayer mediaPlayer; 

    @FXML
    public void initialize() {
        // player name will be retained if babalik sa home screen
        if (Main.playerName != null && !Main.playerName.isEmpty()) {
            nameField.setText(Main.playerName);
        }

        // VIDEO BG
        try {
            URL videoUrl = getClass().getResource("/com/sightreading/videos/background.mp4");
            if (videoUrl != null) {
                Media media = new Media(videoUrl.toExternalForm());
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // loop bg indefinitely
                bgMediaView.setMediaPlayer(mediaPlayer);
                mediaPlayer.play();
            } else {
                System.err.println("Background video not found! Check the path.");
            }
        } catch (Exception e) {
            System.err.println("Could not load video: " + e.getMessage());
        }
    }

    private void stopVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();     // stop the video when leaving the screen
            mediaPlayer.dispose();
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
            stopVideo(); // stop video before switching scenes
            try {
                Main.setRoot("song-list");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleLeaderboard(ActionEvent event) {
        stopVideo(); // stop video before switching scenes
        try {
            Main.setRoot("leaderboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}