package com.sightreading;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.List;

public class LeaderboardController {

    @FXML private VBox scoresContainer;

    @FXML
    public void initialize() {
        List<LeaderboardEntry> scores = LeaderboardManager.getScores();

        if (scores.isEmpty()) {
            Label noScores = new Label("No scores yet. Play a game to set the record!");
            noScores.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 20px;");
            scoresContainer.getChildren().add(noScores);
        } else {
            // display only top 10 scores
            int limit = Math.min(scores.size(), 10);
            for (int i = 0; i < limit; i++) {
                LeaderboardEntry entry = scores.get(i);
                
                String text = (i + 1) + ". " + entry.playerName + " - " + entry.score;
                Label scoreLabel = new Label(text);
                
                if (i == 0) {
                    scoreLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 28px; -fx-font-weight: bold;");
                } else {
                    scoreLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");
                }
                
                scoresContainer.getChildren().add(scoreLabel);
            }
        }
    }

    @FXML
    private void handleBack() {
        try {
            Main.setRoot("home");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}