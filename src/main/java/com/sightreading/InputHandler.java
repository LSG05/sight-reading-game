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
    }
}
