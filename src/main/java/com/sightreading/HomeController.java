package com.sightreading;

import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HomeController {

    @FXML private TextField nameField;
    @FXML private Label errorLabel;
    
    // link to the transparent Pane in FXML to hold particles
    @FXML private Pane particlePane; 

    // FXML variables for the tutorial pane
    @FXML private VBox howToPlayOverlay;      // pop-up container for tutorial
    @FXML private ImageView tutorialImageView; // tutorial pane
    @FXML private ImageView navArrowImageView; // arrow button for navigation

    private int tutorialPage = 1; // page tracker for tutorial navigation

    private List<Particle> particles = new ArrayList<>();
    private AnimationTimer particleTimer;
    private Random random = new Random();

    @FXML
    public void initialize() {
        if (Main.playerName != null && !Main.playerName.isEmpty()) {
            nameField.setText(Main.playerName);
        }
        
        createParticles();
    }

    /**
     * Generates the glowing orbs and starts the animation loop.
     */
    private void createParticles() {
        for (int i = 0; i < 60; i++) {
            // random size between 1 and 3 pixels
            double radius = random.nextDouble() * 2 + 1;
            
            Circle circle = new Circle(radius, Color.web("#ffd700"));
            circle.setEffect(new DropShadow(10, Color.web("#ffd700")));
            
            // spawn randomly across the screen
            circle.setLayoutX(random.nextDouble() * 1200);
            circle.setLayoutY(random.nextDouble() * 900);
            
            // speed 
            double vx = (random.nextDouble() - 0.5) * 0.5; 
            double vy = (random.nextDouble() - 0.5) * 0.5;

            Particle particle = new Particle(circle, vx, vy);
            particles.add(particle);
            particlePane.getChildren().add(circle);
        }

        // updates every particle 60 times a second
        particleTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                for (Particle p : particles) {
                    p.update();
                }
            }
        };
        particleTimer.start();
    }

    /**
     * class handles individual particle physics and twinkling.
     */
    private class Particle {
        Circle node;
        double vx, vy;
        double life;
        double twinkleSpeed;

        Particle(Circle node, double vx, double vy) {
            this.node = node;
            this.vx = vx;
            this.vy = vy;
            // randomize starting twinkle phase and speed
            this.life = random.nextDouble() * Math.PI * 2; 
            this.twinkleSpeed = 0.02 + random.nextDouble() * 0.03; 
        }

        void update() {
            // 1. Move the particle
            node.setLayoutX(node.getLayoutX() + vx);
            node.setLayoutY(node.getLayoutY() + vy);

            if (node.getLayoutX() < 0) node.setLayoutX(1200);
            if (node.getLayoutX() > 1200) node.setLayoutX(0);
            if (node.getLayoutY() < 0) node.setLayoutY(900);
            if (node.getLayoutY() > 900) node.setLayoutY(0);

            // 2. Twinkle Effect 
            life += twinkleSpeed;
            double opacity = Math.abs(Math.sin(life)) * 0.7 + 0.1; // between 0.1 and 0.8 opacity
            node.setOpacity(opacity);
        }
    }

    /**
     * STOPS the particle timer to save memory when leaving the home screen.
     */
    private void cleanup() {
        if (particleTimer != null) {
            particleTimer.stop();
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
            cleanup(); // remove particles when switching scenes
            try {
                Main.setRoot("song-list");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleLeaderboard(ActionEvent event) {
        cleanup(); 
        try {
            Main.setRoot("leaderboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // methods for how to tutorial pop-up
    @FXML
    private void handleHowToPlay() {
        tutorialPage = 1;
        updateTutorialUI();
        howToPlayOverlay.setVisible(true); // show pop-up
    }

    @FXML
    private void handleTutorialNav() {
        if (tutorialPage == 1) {
            tutorialPage = 2;
        } else {
            tutorialPage = 1;
        }
        updateTutorialUI();
    }

    // helper function to update photos
    private void updateTutorialUI() {
        String mainImg;
        String arrowImg;

        if (tutorialPage == 1) {
            mainImg = "/com/sightreading/images/howTo_1.png";
            arrowImg = "/com/sightreading/images/forward_arrow.png"; 
        } else {
            mainImg = "/com/sightreading/images/howTo_2.png";
            arrowImg = "/com/sightreading/images/back_arrow.png";    
        }

    // set images set as imageview in fxml
    tutorialImageView.setImage(new Image(getClass().getResourceAsStream(mainImg)));
    navArrowImageView.setImage(new Image(getClass().getResourceAsStream(arrowImg)));
    }

    @FXML
    private void handleHome() {
        howToPlayOverlay.setVisible(false); // hide pop-up
    }
}