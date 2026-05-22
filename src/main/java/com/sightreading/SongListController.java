package com.sightreading;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class SongListController {

    @FXML private HBox carouselBox;
    @FXML private ScrollPane carouselScrollPane;
    @FXML private Button backButton;  
    
    // particles
    @FXML private Pane particlePane;
    private List<Particle> particles = new ArrayList<>();
    private AnimationTimer particleTimer;
    private Random random = new Random();

    // audio
    private AudioClip hoverSound;
    private AudioService previewService;

    @FXML
    public void initialize() {
        createParticles();

        // sfx loader
        try {
            URL soundUrl = getClass().getResource("/com/sightreading/audio/click.mp3");
            if (soundUrl != null) {
                String source = soundUrl.toExternalForm();
                System.out.println("Loading audio from: " + source);
                hoverSound = new AudioClip(source);
            } else {
                System.err.println("CRITICAL: click.mp3 not found in resources!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // parse catalog
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("/com/sightreading/catalog.json")))) {
            
            Gson gson = new Gson();
            List<SongData> songs = gson.fromJson(reader, new TypeToken<List<SongData>>(){}.getType());

            for (SongData song : songs) {
                addSongCard(song.title, song.bpm, song.imagePath, song.id, false);
            }

        } catch (Exception e) {
            System.err.println("Error reading catalog.json. Using hardcoded emergency safety fallback.");
            addSongCard("Twinkle Twinkle", "80 BPM", "/com/sightreading/songs/twinkle/cover.jpg", "twinkle", false);
            addSongCard("Pirates of the Caribbean", "100 BPM", null, "pirates", false);
        }
        
        // locked cards
        addSongCard("Rainbow", "", "/com/sightreading/images/rainbow.png", "rainbow", true);
        addSongCard("Canon", "", "/com/sightreading/images/canon.png", "canon", true);


        // back button logic
        try {
            Image btnImage = new Image(getClass().getResourceAsStream("/com/sightreading/images/back_button.png"));
            ImageView btnView = new ImageView(btnImage);
            
            btnView.setFitHeight(80); 
            btnView.setPreserveRatio(true);
        
            backButton.setGraphic(btnView);
            backButton.setText(""); 
            backButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        
            backButton.setOnMouseEntered(e -> {
                playHoverSound();
                btnView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(68,136,255,0.6), 12, 0, 0, 0);");
                btnView.setTranslateY(-2); 
            });
            
            backButton.setOnMouseExited(e -> {
                btnView.setStyle("");
                btnView.setTranslateY(0);
            });
            
            backButton.setOnMousePressed(e -> btnView.setTranslateY(2)); 
            backButton.setOnMouseReleased(e -> btnView.setTranslateY(-2));
        
        } catch (Exception e) {
            System.err.println("Could not load custom Canva button asset. Check the file path.");
        }

        // lock on card
        Platform.runLater(() -> {
            if (!carouselBox.getChildren().isEmpty()) {
                StackPane firstCard = (StackPane) carouselBox.getChildren().get(0);
                firstCard.requestFocus();
                updateCarouselFocus(firstCard); 
            }
        });
    }

    // carousel logic

    private void addSongCard(String title, String subtitle, String imagePath, String songId, boolean isLocked) {
        StackPane cardRoot = new StackPane();
        cardRoot.setPrefSize(300, 420);
        cardRoot.setFocusTraversable(true);
        cardRoot.setStyle("-fx-background-color: transparent;");

        ImageView fullCardView = new ImageView();
        fullCardView.setFitWidth(300);
        fullCardView.setFitHeight(420);
        fullCardView.setPreserveRatio(true);
        
        if (imagePath != null) {
            try {
                fullCardView.setImage(new Image(getClass().getResourceAsStream(imagePath)));
            } catch (Exception e) {
                System.err.println("Could not load baked Canva card: " + imagePath);
            }
        }

        cardRoot.getChildren().add(fullCardView);

        // unselected state (smaller and faded)
        cardRoot.setScaleX(0.85);
        cardRoot.setScaleY(0.85);
        cardRoot.setOpacity(0.6);

        // mouse hover
        cardRoot.setOnMouseEntered(e -> {
            if (!cardRoot.isFocused()) {
                cardRoot.setScaleX(0.9);
                cardRoot.setScaleY(0.9);
                cardRoot.setOpacity(0.8);
            }
            playHoverSound();

            if (!isLocked) {
                if (previewService != null) previewService.stopSong();
                String previewPath = "/com/sightreading/songs/" + songId + "/audio.wav";
                previewService = new AudioService(previewPath, null);
                previewService.loadSong();
                previewService.playPreview(5.0);
            }
        });
        
        cardRoot.setOnMouseExited(e -> {
            if (!cardRoot.isFocused()) {
                cardRoot.setScaleX(0.85);
                cardRoot.setScaleY(0.85);
                cardRoot.setOpacity(0.6);
            }
            // stop preview when going to another song
            if (!cardRoot.isFocused() && previewService != null) {
                previewService.stopSong();
            }
        });

        // focus
        cardRoot.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                updateCarouselFocus(cardRoot); // glides the carousel to center
                playHoverSound();
                
                if (!isLocked) {
                    if (previewService != null) previewService.stopSong();
                    String previewPath = "/com/sightreading/songs/" + songId + "/audio.wav";
                    previewService = new AudioService(previewPath, null);
                    previewService.loadSong();
                    previewService.playPreview(5.0);
                }
            } else {
                // if it loses focus, stop its song
                if (previewService != null) previewService.stopSong();
            }
        });

        // click to start / focus (if locked)
        cardRoot.setOnMouseClicked(e -> {
            if (!cardRoot.isFocused()) {
                cardRoot.requestFocus(); // slides card to center
            } else if (!isLocked) {
                startGame(songId); // if it's already centered, launch game (if unlocked)
            }
        });

        // KEYBOARD NAVIGATION
        cardRoot.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !isLocked) { 
                startGame(songId);  
            } else if (e.getCode() == KeyCode.RIGHT) {  
                focusNext(cardRoot);    
            } else if (e.getCode() == KeyCode.LEFT) {   
                focusPrevious(cardRoot);    
            }
        });

        carouselBox.getChildren().add(cardRoot);
    }

    /**
     * shrink other cards, enlarge focused
     */
    private void updateCarouselFocus(StackPane activeCard) {
        // 1. scale sizes 
        for (javafx.scene.Node node : carouselBox.getChildren()) {
            StackPane card = (StackPane) node;
            if (card == activeCard) {
                card.setScaleX(1.15); 
                card.setScaleY(1.15);
                card.setOpacity(1.0);
                card.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(68,136,255,0.7), 25, 0, 0, 0);");
            } else {
                card.setScaleX(0.85); 
                card.setScaleY(0.85);
                card.setOpacity(0.6);
                card.setStyle("");
            }
        }
        
        Platform.runLater(() -> {
            double contentWidth = carouselScrollPane.getContent().getBoundsInLocal().getWidth();
            double viewportWidth = carouselScrollPane.getViewportBounds().getWidth();
            
            if (contentWidth <= viewportWidth) return;
            
            double cardX = activeCard.getBoundsInParent().getMinX();
            double cardWidth = activeCard.getBoundsInParent().getWidth();
            
            double targetHvalue = (cardX - (viewportWidth - cardWidth) / 2) / (contentWidth - viewportWidth);
            targetHvalue = Math.max(0, Math.min(1, targetHvalue));
            
            Timeline timeline = new Timeline();
            KeyValue kv = new KeyValue(carouselScrollPane.hvalueProperty(), targetHvalue, javafx.animation.Interpolator.EASE_BOTH);
            KeyFrame kf = new KeyFrame(Duration.millis(250), kv);
            timeline.getKeyFrames().add(kf);
            timeline.play();
        });
    }

    // INFINITE LOOP LOGIC

    private void focusNext(StackPane currentCard) {
        int index = carouselBox.getChildren().indexOf(currentCard);
        if (index < carouselBox.getChildren().size() - 1) {
            carouselBox.getChildren().get(index + 1).requestFocus();
        } else {
            carouselBox.getChildren().get(0).requestFocus(); // Wrap to start
        }
    }

    private void focusPrevious(StackPane currentCard) {
        int index = carouselBox.getChildren().indexOf(currentCard);
        if (index > 0) {
            carouselBox.getChildren().get(index - 1).requestFocus();
        } else {
            carouselBox.getChildren().get(carouselBox.getChildren().size() - 1).requestFocus(); // Wrap to end
        }
    }

    // GAME START
    private void startGame(String songId) {
        System.out.println("Starting song: " + songId);
        Main.selectedSongId = songId;       
        cleanup(); 
        try {
            Main.setRoot("game");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        cleanup(); 
        try {
            Main.setRoot("home");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cleanup() {
        if (particleTimer != null) particleTimer.stop();
        if (hoverSound != null) hoverSound.stop();
        if (previewService != null) previewService.stopSong();
    }

    // SFX Helper
    private void playHoverSound() {
        if (hoverSound != null) {
            hoverSound.play(); 
        }
    }

    // PARTICLES
    private void createParticles() {
        for (int i = 0; i < 60; i++) {
            double radius = random.nextDouble() * 2 + 1;
            Circle circle = new Circle(radius, Color.web("#ffd700")); 
            circle.setEffect(new DropShadow(10, Color.web("#ffd700")));
            
            circle.setLayoutX(random.nextDouble() * 1200);
            circle.setLayoutY(random.nextDouble() * 900);
            
            double vx = (random.nextDouble() - 0.5) * 0.5; 
            double vy = (random.nextDouble() - 0.5) * 0.5;

            Particle particle = new Particle(circle, vx, vy);
            particles.add(particle);
            particlePane.getChildren().add(circle);
        }

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

    private class Particle {
        Circle node;
        double vx, vy;
        double life;
        double twinkleSpeed;

        Particle(Circle node, double vx, double vy) {
            this.node = node;
            this.vx = vx;
            this.vy = vy;
            this.life = random.nextDouble() * Math.PI * 2; 
            this.twinkleSpeed = 0.02 + random.nextDouble() * 0.03; 
        }

        void update() {
            node.setLayoutX(node.getLayoutX() + vx);
            node.setLayoutY(node.getLayoutY() + vy);

            if (node.getLayoutX() < 0) node.setLayoutX(1200);
            if (node.getLayoutX() > 1200) node.setLayoutX(0);
            if (node.getLayoutY() < 0) node.setLayoutY(900);
            if (node.getLayoutY() > 900) node.setLayoutY(0);

            life += twinkleSpeed;
            double opacity = Math.abs(Math.sin(life)) * 0.7 + 0.1; 
            node.setOpacity(opacity);
        }
    }

    private static class SongData {
        String id;
        String title;
        String bpm;
        String imagePath;
    }
}