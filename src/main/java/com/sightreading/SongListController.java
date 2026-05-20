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
    private void addSongCard(String title, String subtitle, String imagePath, String songId) {
        VBox card = new VBox();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(15);
        card.setPrefSize(300, 400);
        
        String defaultStyle = "-fx-background-color: #1E1E24; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5);";
        // glow when hovered
        String hoverStyle = "-fx-background-color: #2A2A35; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, #4488ff, 15, 0, 0, 0); -fx-scale-x: 1.05; -fx-scale-y: 1.05;";
        
        card.setStyle(defaultStyle);

        // song preview image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(200);
        imageView.setFitHeight(200);
        imageView.setPreserveRatio(true);
        
        if (imagePath != null) {
            try {
                imageView.setImage(new Image(getClass().getResourceAsStream(imagePath)));
            } catch (Exception e) {
                System.err.println("Could not load image: " + imagePath);
            }
        }

        // labels
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        
        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 16px;");

        // song card selection 
        card.getChildren().addAll(imageView, titleLabel, subLabel);
        card.setFocusTraversable(true);
        
        // mouse hover and trigger
        card.setOnMouseEntered(e -> {
            card.setStyle(hoverStyle);
            if (hoverSound != null) {
                hoverSound.setFramePosition(0); // rewind sound
                hoverSound.start();             // play clip natively
            }
            card.requestFocus();    // syncs keyboard and mouse hover
        });
        
        card.setOnMouseExited(e -> card.setStyle(defaultStyle));

        card.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                card.setStyle(hoverStyle);
                double hvalue = carouselBox.getChildren().indexOf(card) / (double) (Math.max(1, carouselBox.getChildren().size() - 1));
                carouselScrollPane.setHvalue(hvalue);
            } else {
                card.setStyle(defaultStyle);
            }
        });

        // mouse click to play selected song
        card.setOnMouseClicked(e -> startGame(songId));

        // scrolling using keyboard
        // KEYBOARD CONTROLLS
        card.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) { // enter key to play selected song
                startGame(songId);  
            } else if (e.getCode() == KeyCode.RIGHT) {  // right arrow key to play selected song
                focusNext(card);    
            } else if (e.getCode() == KeyCode.LEFT) {   // left arrow key to play selected song
                focusPrevious(card);    
            }
        });

        carouselBox.getChildren().add(card);
    }

    private void focusNext(VBox currentCard) {
        int index = carouselBox.getChildren().indexOf(currentCard);
        if (index < carouselBox.getChildren().size() - 1) {
            carouselBox.getChildren().get(index + 1).requestFocus();
        }
    }

    private void focusPrevious(VBox currentCard) {
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