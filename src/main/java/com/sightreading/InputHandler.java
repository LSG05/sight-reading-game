package com.sightreading;

import javafx.scene.input.KeyEvent;

// Responsible for keyboard/input events
public class InputHandler {
    private final GameController gameController;
    
    public InputHandler(GameController gameController) {
        this.gameController = gameController;
    }

    public void handleKeyPressed(KeyEvent event) {
        String keyPressed = event.getText().toUpperCase();
        System.out.println("Key pressed: '" + keyPressed + "' (code: " + event.getCode() + ")");
       
        long currentTime = (long) gameController.getAudioService().getCurrentTimeMs(); 
        NoteData currentNote = gameController.getCurrentNote();
       
        System.out.println("Current note: " + (currentNote != null ? currentNote.noteName : "null") + ", processed: " + (currentNote != null ? currentNote.processed : "N/A"));
        System.out.println("DEBUG: Key Pressed: " + keyPressed + " | Target Note: " + currentNote.noteName + " (elapsed: " + currentTime + "ms, target: " + currentNote.targetTimeMs + "ms)");
    
        // Anti-Cheat: Only allow hit if note exists and isn't processed
        if (currentNote == null || currentNote.processed) {
            gameController.getScoreManager().registerStray(); 
            gameController.triggerMissFeedback(); // Flashes red for button mashing
            return;
        }

        // Key Validation: Check if the key matches the note name
        if (currentNote.noteName.equalsIgnoreCase(keyPressed)) {
            // Calculate timing error
            long timingError = currentTime - currentNote.targetTimeMs;

            // Register the hit
            currentNote.processed = true;
            currentNote.isHit = true;
            gameController.getScoreManager().registerHit(timingError);
            gameController.triggerHitFeedback(); // Flashes blue for success
            System.out.println("HIT registered for note: " + currentNote.noteName);
        } else {
            // If the key doesn't match, it's a miss
            currentNote.processed = true;
            currentNote.isHit = false;
            gameController.getScoreManager().registerMiss();
            gameController.triggerMissFeedback(); // Flashes red for mistake
            System.out.println("MISS registered for note: " + currentNote.noteName + " (pressed: " + keyPressed + ")");
        }
    }
}