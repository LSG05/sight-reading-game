package com.sightreading;

import javafx.application.Platform;

public class ScoreManager {
    private int score = 0;
    private int combo = 0;
    private int multiplier = 1;
    private GameController gameController;

    public ScoreManager(GameController gameController) {
        this.gameController = gameController;
    }
    
    public void registerHit(long timingError){
        String rating = "OKAY";
        long absError = Math.abs(timingError);

        // update rating based on timing error categories
        //UPDATE: fixed logic so some early hits are not registered as perfect
        if (absError <= 50) {
            rating = "PERFECT";
        } else if (absError <= 100) {
            rating = "GREAT";
        } else if (absError <= 190){
            rating = (timingError <0) ? "EARLY" : "LATE";
        } else {
            rating = "OKAY";
        }

        // penalty based on timing error (linear scale)
        double penalty = (absError / 190.0) * 80.0;
        int basePoints = (int) (100 - penalty);

        // multiplier logic based on current combo
        this.multiplier = (combo / 10) + 1;
        if (this.multiplier > 10) {
            this.multiplier = 10; // Cap multiplier at 10x
        }

        // score update
        this.score += (basePoints * multiplier);
        this.combo++;

        // Update UI
        // final to "lock" values in temporarily while inserted into platform runlater function
        final int currentScore = this.score;
        final int currentCombo = this.combo;
        final String currentRating = rating;

        Platform.runLater(() -> {
            gameController.updateUI(currentScore, currentCombo, currentRating); // method to be updated later
        });

    }

    public void registerMiss() {
        this.combo = 0;
        this.multiplier = 1;
        this.score =  Math.max(0, this.score - 50); // flat penalty for miss

        Platform.runLater(() -> {
            gameController.updateUI(this.score, this.combo, "MISS");
        });
    }

    public void registerStray() {
        this.combo = 0;
        this.multiplier = 1;
        this.score =  Math.max(0, this.score - 15); // smaller penalty for stray notes

        Platform.runLater(() -> {
            gameController.updateUI(this.score, this.combo, "STRAY");
        });
    }
}
