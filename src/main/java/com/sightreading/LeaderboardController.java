package com.sightreading;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import java.io.IOException;
import java.util.List;

public class LeaderboardController {

    @FXML private VBox scoresContainer;

    @FXML
    public void initialize() {
        List<LeaderboardEntry> scores = LeaderboardManager.getScores();
        scoresContainer.getChildren().clear(); 

        // table headers
        HBox header = createRow("RANK", "PLAYER NAME", "HIGHEST SCORE");
        header.setStyle("-fx-border-color: transparent transparent #555555 transparent; -fx-border-width: 0 0 2 0; -fx-padding: 0 0 10 0;");
        
        // text style
        for(javafx.scene.Node node : header.getChildren()) {
            if(node instanceof Label) {
                ((Label) node).setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: 'Georgia', serif;");
            }
        }
        scoresContainer.getChildren().add(header);

        // POPULATE ROWS
        if (scores.isEmpty()) {
            Label noScores = new Label("No scores yet. Play a game to set the record!");
            // Changed to Georgia, serif
            noScores.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 20px; -fx-padding: 30 0 0 0; -fx-font-family: 'Georgia', serif;");
            scoresContainer.getChildren().add(noScores);
        } else {
            // display only top 10 scores
            int limit = Math.min(scores.size(), 10);
            for (int i = 0; i < limit; i++) {
                LeaderboardEntry entry = scores.get(i);
                
                // crown for first place
                String rankText = (i == 0) ? "👑 1" : String.valueOf(i + 1);
                
                // build the perfectly aligned row
                HBox row = createRow(rankText, entry.playerName, String.valueOf(entry.score));
                
                // styling
                String rowStyle;
                if (i == 0) {
                    // GOLD with a glowing drop shadow for 1st
                    rowStyle = "-fx-text-fill: #ffd700; -fx-font-size: 26px; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(255,215,0,0.4), 10, 0, 0, 0);"; 
                } else if (i == 1) {
                    // SILVER for 2nd
                    rowStyle = "-fx-text-fill: #C0C0C0; -fx-font-size: 24px; -fx-font-weight: bold;"; 
                } else if (i == 2) {
                    // BRONZE for 3rd
                    rowStyle = "-fx-text-fill: #CD7F32; -fx-font-size: 24px; -fx-font-weight: bold;"; 
                } else {
                    // white for the rest
                    rowStyle = "-fx-text-fill: white; -fx-font-size: 22px;"; 
                }
                
                // apply the style to all 3 columns in this row
                for(javafx.scene.Node node : row.getChildren()) {
                    if(node instanceof Label) {
                        ((Label) node).setStyle(rowStyle + " -fx-font-family: 'Georgia', serif;");
                    }
                }
                
                scoresContainer.getChildren().add(row);
            }
        }
    }

    // horizontal box with strict column widths
    private HBox createRow(String col1, String col2, String col3) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(20);
        
        // rank
        Label lbl1 = new Label(col1);
        lbl1.setPrefWidth(120);
        lbl1.setAlignment(Pos.CENTER);
        
        // player name
        Label lbl2 = new Label(col2);
        lbl2.setPrefWidth(350);
        lbl2.setAlignment(Pos.CENTER_LEFT);
        
        // highest score of player
        Label lbl3 = new Label(col3);
        lbl3.setPrefWidth(200);
        lbl3.setAlignment(Pos.CENTER_RIGHT);
        
        row.getChildren().addAll(lbl1, lbl2, lbl3);
        return row;
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