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
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;

public class SongListController {

    @FXML private HBox carouselBox;
    @FXML private ScrollPane carouselScrollPane;
    @FXML private Button backButton;  // return to home
    
    @FXML private Pane particlePane;
    private List<Particle> particles = new ArrayList<>();
    private AnimationTimer particleTimer;
    private Random random = new Random();

    private AudioClip hoverSound;
    private AudioService previewService; // for song preview on hover
    // outdated: private Clip hoverSound; // so that native audio file will be used

    @FXML
    public void initialize() {
        // floating particles
        createParticles();

        try {
            URL soundUrl = getClass().getResource("/com/sightreading/audio/click.mp3");
            if (soundUrl != null) {
                String source = soundUrl.toExternalForm();
                System.out.println("Loading audio from: " + source); // Check your console for this!
                hoverSound = new AudioClip(source);
            } else {
                System.err.println("CRITICAL: click.mp3 not found in resources!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* outdated hover sound effect
        try {
            URL soundUrl = getClass().getResource("/com/sightreading/audio/tick.wav");  // folder location
            if (soundUrl != null) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundUrl);
                hoverSound = AudioSystem.getClip();
                hoverSound.open(audioStream);
            }
        } catch (Exception e) {
            System.err.println("Native audio engine failed to open asset. Skipping hover audio.");
        }
            */

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("/com/sightreading/catalog.json")))) {
            
            Gson gson = new Gson();
            List<SongData> songs = gson.fromJson(reader, new TypeToken<List<SongData>>(){}.getType());

            // CAROUSEL
            // add song cards one by one
            for (SongData song : songs) {
                addSongCard(song.title, song.bpm, song.imagePath, song.id, false);
            }

        } catch (Exception e) {
            System.err.println("Error reading catalog.json. Using hardcoded emergency safety fallback.");
            addSongCard("Twinkle Twinkle", "80 BPM", "/com/sightreading/songs/twinkle/cover.jpg", "twinkle", false);
            addSongCard("Pirates of the Caribbean", "100 BPM", null, "pirates", false);
        }

        addSongCard("Rainbow", "", "/com/sightreading/images/rainbow.png", "rainbow", true);
        addSongCard("Canon", "", "/com/sightreading/images/canon.png", "canon", true);

        try {
            // BUTTON
            Image btnImage = new Image(getClass().getResourceAsStream("/com/sightreading/images/back_button.png"));
            ImageView btnView = new ImageView(btnImage);
            
            // dimensions
            btnView.setFitHeight(80); 
            btnView.setPreserveRatio(true);
        
            backButton.setGraphic(btnView);
            backButton.setText(""); // Wipe the text so only your image shows
            backButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        
            // visual effects
            backButton.setOnMouseEntered(e -> {
                playHoverSound(); // call helper for sfx
                btnView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(68,136,255,0.6), 12, 0, 0, 0);");
                btnView.setTranslateY(-2); 
            });
            
            backButton.setOnMouseExited(e -> {
                btnView.setStyle("");
                btnView.setTranslateY(0);
            });
            
            backButton.setOnMousePressed(e -> btnView.setTranslateY(2)); // Pushes down when clicked
            backButton.setOnMouseReleased(e -> btnView.setTranslateY(-2));
        
        } catch (Exception e) {
            System.err.println("Could not load custom Canva button asset. Check the file path.");
        }

        // set first card as the default
        javafx.application.Platform.runLater(() -> {
            if (!carouselBox.getChildren().isEmpty()) {
                StackPane firstCard = (StackPane) carouselBox.getChildren().get(0);
                firstCard.requestFocus();
                updateCarouselFocus(firstCard);
            }
        });
    }

    // PARTICLES
    private void createParticles() {
        for (int i = 0; i < 60; i++) {
            double radius = random.nextDouble() * 2 + 1;
            Circle circle = new Circle(radius, Color.web("#ffd700")); // Gold
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

    private void cleanup() {
        if (particleTimer != null) particleTimer.stop();
        if (hoverSound != null) hoverSound.stop();
        if (previewService != null) {
                previewService.stopSong();
        }
    }

    // vertical box for each song card
    // LAYOUT
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
        
        cardRoot.setScaleX(0.85);
        cardRoot.setScaleY(0.85);
        cardRoot.setOpacity(0.6);

        cardRoot.setOnMouseEntered(e -> {
            if (!cardRoot.isFocused()) {
                cardRoot.setScaleX(0.9);
                cardRoot.setScaleY(0.9);
                cardRoot.setOpacity(0.8);
            }
            playHoverSound(); // add hover sound effect

            // hover preview music
            if (!isLocked) {
                if (previewService != null) {
                    previewService.stopSong();
                }

                String previewPath = "/com/sightreading/songs/" + songId + "/audio.wav";
                // We pass 'null' for GameController because we don't want game logic in the menu
                previewService = new AudioService(previewPath, null); 
                previewService.loadSong();
                previewService.playPreview(5.0); // Play for 5 seconds
            }
        });
        
        cardRoot.setOnMouseExited(e -> {
            if (!cardRoot.isFocused()) {
                cardRoot.setScaleX(0.85);
                cardRoot.setScaleY(0.85);
                cardRoot.setOpacity(0.6);
            }

            // song preview cleanup
            if (!cardRoot.isFocused() && previewService != null) {
                previewService.stopSong();
            }
        });

        cardRoot.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                updateCarouselFocus(cardRoot);
                playHoverSound(); // add hover sound effect
                if (!isLocked) {
                    if (previewService != null) {
                        previewService.stopSong();
                    }
                    String previewPath = "/com/sightreading/songs/" + songId + "/audio.wav";
                    // We pass 'null' for GameController because we don't want game logic in the menu
                    previewService = new AudioService(previewPath, null); 
                    previewService.loadSong();
                    previewService.playPreview(5.0); // Play for 5 seconds
                }
            } else {
                // song preview cleanup
                if (previewService != null) {
                    previewService.stopSong();
                }
            }
        });

        cardRoot.setOnMouseClicked(e -> {
            if (cardRoot.getScaleX() > 1.1) {
                if (!isLocked) {
                    startGame(songId); 
                }
            } else {
                cardRoot.requestFocus(); 
            }
        });

        cardRoot.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) { // enter key to play selected song
                if (!isLocked) startGame(songId);  
            } else if (e.getCode() == KeyCode.RIGHT) {  // right arrow key to play selected song
                focusNext(cardRoot);    
            } else if (e.getCode() == KeyCode.LEFT) {   // left arrow key to play selected song
                focusPrevious(cardRoot);    
            }
        });

        carouselBox.getChildren().add(cardRoot);
    }

    private void updateCarouselFocus(StackPane activeCard) {
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

    private void focusNext(StackPane currentCard) {
        int index = carouselBox.getChildren().indexOf(currentCard);
        if (index < carouselBox.getChildren().size() - 1) {
            carouselBox.getChildren().get(index + 1).requestFocus();
        } else {
            carouselBox.getChildren().get(0).requestFocus();
        }
    }

    private void focusPrevious(StackPane currentCard) {
        int index = carouselBox.getChildren().indexOf(currentCard);
        if (index > 0) {
            carouselBox.getChildren().get(index - 1).requestFocus();
        } else {
            carouselBox.getChildren().get(carouselBox.getChildren().size() - 1).requestFocus();
        }
    }

    private void startGame(String songId) {
        System.out.println("Starting song: " + songId);
        Main.selectedSongId = songId;       // song will change based on song ID
        cleanup(); // stop particles
        try {
            Main.setRoot("game");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        cleanup(); // stop particles
        try {
            Main.setRoot("home");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class SongData {
        String id;
        String title;
        String bpm;
        String imagePath;
    }

    // for hover sound
    private void playHoverSound() {
        if (hoverSound != null) {
            hoverSound.play(); 
        }
    }
}