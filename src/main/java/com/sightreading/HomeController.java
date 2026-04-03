package com.sightreading;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import java.io.IOException;

public class HomeController {

    @FXML
    private void handlePlay(ActionEvent event) {
        System.out.println("Play button clicked. Routing to song list...");
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