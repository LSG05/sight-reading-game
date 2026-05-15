package com.sightreading;

import javafx.scene.input.KeyEvent;

// responsible for keyboard/input events
public class InputHandler {
    private final GameController gameController;
    private static final int HIT_TOLERANCE_PX = 50;  // How close the hitbox needs to be
    
    public InputHandler(GameController gameController) {
        this.gameController = gameController;
    }

    // to fill out in phase 3
    public void handleKeyPressed(KeyEvent event) {
        String keyPressed = event.getText().toUpperCase();
        System.out.println("Key pressed: '" + keyPressed + "' (code: " + event.getCode() + ")");
       
        long currentTime = gameController.getMasterClock().getElapsedMs();
        NoteData currentNote = gameController.getCurrentNote();
       
        System.out.println("Current note: " + (currentNote != null ? currentNote.noteName : "null") + ", processed: " + (currentNote != null ? currentNote.processed : "N/A"));


        // 1. Anti-Cheat: Only allow hit if note exists and isn't processed
        if (currentNote == null || currentNote.processed) {
            return;
        }


        // 2. Key Validation: Check if the key matches the note name
        if (currentNote.noteName.equalsIgnoreCase(keyPressed) ) {
        
            // 3. Calculate timing error (Time they pressed - Time they should have pressed)
            long timingError = currentTime - currentNote.targetTimeMs;


            // 4. Register the hit using YOUR specific logic
            currentNote.processed = true;
            currentNote.isHit = true;
            gameController.getScoreManager().registerHit(timingError);
            System.out.println("HIT registered for note: " + currentNote.noteName);
        } 
        
        else {
            // If the key doesn't match, it's a miss
            currentNote.processed = true;
            currentNote.isHit = false;
            gameController.getScoreManager().registerMiss();
            System.out.println("MISS registered for note: " + currentNote.noteName + " (pressed: " + keyPressed + ")");
        }

        System.out.println("DEBUG: Key Pressed: " + keyPressed + " | Target Note: " + currentNote.noteName + " (elapsed: " + currentTime + "ms, target: " + currentNote.targetTimeMs + "ms)");
    }
}
