package com.sightreading;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;

public class SongListController {

    @FXML private HBox carouselBox;
    @FXML private ScrollPane carouselScrollPane;

    private Clip hoverSound; // so that native audio file will be used

    @FXML
    public void initialize() {
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

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("/com/sightreading/catalog.json")))) {
            
            Gson gson = new Gson();
            List<SongData> songs = gson.fromJson(reader, new TypeToken<List<SongData>>(){}.getType());

            // CAROUSEL
            // add song cards one by one
            for (SongData song : songs) {
                addSongCard(song.title, song.bpm, song.imagePath, song.id);
            }

        } catch (Exception e) {
            System.err.println("Error reading catalog.json. Using hardcoded emergency safety fallback.");
            addSongCard("Twinkle Twinkle", "80 BPM", "/com/sightreading/songs/twinkle/cover.jpg", "twinkle");
            addSongCard("Pirates of the Caribbean", "100 BPM", null, "pirates");
        }

        // set first card as the default
        javafx.application.Platform.runLater(() -> {
            if (!carouselBox.getChildren().isEmpty()) {
                carouselBox.getChildren().get(0).requestFocus();
            }
        });
    }

    // vertical box for each song card
    // LAYOUT
    private void addSongCard(String title, String subtitle, String imagePath, String songId) {
        StackPane cardRoot = new StackPane();
        cardRoot.setPrefSize(300, 420);
        cardRoot.setFocusTraversable(true);

        // theme colors
        String baseCardStyle = "-fx-background-color: #14141A; -fx-background-radius: 16; -fx-border-color: #2A2A35; -fx-border-radius: 16; -fx-border-width: 1.5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 12, 0, 0, 6);";
        String hoverCardStyle = "-fx-background-color: #1A1A24; -fx-background-radius: 16; -fx-border-color: #4488ff; -fx-border-radius: 16; -fx-border-width: 2; -fx-effect: dropshadow(three-pass-box, rgba(68,136,255,0.4), 20, 0, 0, 0); -fx-scale-x: 1.04; -fx-scale-y: 1.04;";
        
        // split song card
        VBox cardBody = new VBox();
        cardBody.setStyle(baseCardStyle);
        cardBody.setPrefSize(300, 420);
        cardBody.setAlignment(Pos.TOP_CENTER);

        // IMAGE
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(280);
        imageContainer.setAlignment(Pos.CENTER);

        // glow effect
        javafx.scene.shape.Circle glowRing = new javafx.scene.shape.Circle(85);
        glowRing.setStyle("-fx-fill: transparent; -fx-stroke: linear-gradient(from 0% 0% to 100% 100%, #4488ff, transparent); -fx-stroke-width: 1.5; -fx-opacity: 0.4;");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);
        
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(180, 180);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        imageView.setClip(clip);

        if (imagePath != null) {
            try {
                imageView.setImage(new Image(getClass().getResourceAsStream(imagePath)));
            } catch (Exception e) {
                System.err.println("Could not load image: " + imagePath);
            }
        }
        imageContainer.getChildren().addAll(glowRing, imageView);

        // angle for song card
        VBox footerBlock = new VBox();
        footerBlock.setPrefHeight(140);
        footerBlock.setAlignment(Pos.CENTER);
        footerBlock.setSpacing(6);
        
        // asymmetric gradient as bg of lower part
        footerBlock.setStyle("-fx-background-color: linear-gradient(from 0% 25% to 100% 0%, #1E1E24 0%, #252530 100%);" +
                             "-fx-background-radius: 0 0 14 14;" +
                             "-fx-border-color: #3A3A4A transparent transparent transparent;" +
                             "-fx-border-width: 1.5;");

        // labels
        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 900; -fx-letter-spacing: 1px;");
        
        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-text-fill: #4488ff; -fx-font-size: 14px; -fx-font-weight: bold;");

        footerBlock.getChildren().addAll(titleLabel, subLabel);

        cardBody.getChildren().addAll(imageContainer, footerBlock);

        HBox badge = new HBox();
        badge.setAlignment(Pos.CENTER);
        badge.setPrefSize(95, 24);
        badge.setMaxSize(95, 24);
        
        badge.setStyle("-fx-background-color: linear-gradient(to right, #4488ff, #2244aa);" +
                       "-fx-background-radius: 4 12 12 0;" +
                       "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 2, 2);");
        
        Label badgeLabel = new Label("BEGINNER");
        badgeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: 900; -fx-letter-spacing: 0.5px;");
        badge.getChildren().add(badgeLabel);

        cardRoot.getChildren().addAll(cardBody, badge);
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new javafx.geometry.Insets(12, 0, 0, -4)); // Let it hang slightly over the left boundary

        // mouse hover
        cardRoot.setOnMouseEntered(e -> {
            cardBody.setStyle(hoverCardStyle);
            badge.setStyle("-fx-background-color: linear-gradient(to right, #66a3ff, #4488ff); -fx-background-radius: 4 12 12 0;");
            cardRoot.requestFocus();
        });
        
        cardRoot.setOnMouseExited(e -> {
            cardBody.setStyle(baseCardStyle);
            badge.setStyle("-fx-background-color: linear-gradient(to right, #4488ff, #2244aa); -fx-background-radius: 4 12 12 0;");
        });

        cardRoot.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                cardBody.setStyle(hoverCardStyle);
                double hvalue = carouselBox.getChildren().indexOf(cardRoot) / (double) (Math.max(1, carouselBox.getChildren().size() - 1));
                carouselScrollPane.setHvalue(hvalue);
            } else {
                cardBody.setStyle(baseCardStyle);
            }
        });

        cardRoot.setOnMouseClicked(e -> startGame(songId));

        cardRoot.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) { // enter key to play selected song
                startGame(songId);  
            } else if (e.getCode() == KeyCode.RIGHT) {  // right arrow key to play selected song
                focusNext(cardRoot);    
            } else if (e.getCode() == KeyCode.LEFT) {   // left arrow key to play selected song
                focusPrevious(cardRoot);    
            }
        });

        carouselBox.getChildren().add(cardRoot);
    }

    private void focusNext(StackPane currentCard) {
        int index = carouselBox.getChildren().indexOf(currentCard);
        if (index < carouselBox.getChildren().size() - 1) {
            carouselBox.getChildren().get(index + 1).requestFocus();
        }
    }

    private void focusPrevious(StackPane currentCard) {
        int index = carouselBox.getChildren().indexOf(currentCard);
        if (index > 0) {
            carouselBox.getChildren().get(index - 1).requestFocus();
        }
    }

    private void startGame(String songId) {
        System.out.println("Starting song: " + songId);
        try {
            Main.setRoot("game");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
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
}