package com.sightreading;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
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
    @FXML private Button backButton;  // return to home

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

        // 1. Load your Canva Frame (The Bottom Layer)
        ImageView frameView = new ImageView();
        try {
            // Make sure your Canva export is named exactly this and placed in the images folder!
            frameView.setImage(new Image(getClass().getResourceAsStream("/com/sightreading/images/card_frame.png")));
            frameView.setFitWidth(300);
            frameView.setFitHeight(420);
        } catch (Exception e) {
            System.err.println("Could not load Canva card frame. Check the file path.");
        }

        // 2. The Content Container (Holds the cover art and the text)
        VBox contentBox = new VBox();
        contentBox.setPrefSize(300, 420);
        contentBox.setAlignment(Pos.TOP_CENTER);
        
        // Push everything down so it fits perfectly inside the visual borders of your Canva frame
        // (Top padding: 75px pushes art into the blue area)
        contentBox.setPadding(new javafx.geometry.Insets(75, 0, 0, 0)); 
        contentBox.setSpacing(45); // Space between the album art and the text block

        // --- ALBUM ART ---
        ImageView coverView = new ImageView();
        coverView.setFitWidth(150);
        coverView.setFitHeight(150);
        coverView.setPreserveRatio(true);
        
        // Keep the rounded corners for the album art so it blends well inside the frame
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(150, 150);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        coverView.setClip(clip);

        if (imagePath != null) {
            try {
                coverView.setImage(new Image(getClass().getResourceAsStream(imagePath)));
            } catch (Exception e) {
                System.err.println("Could not load image: " + imagePath);
            }
        }

        // REMINDER: will change this!!  -----------------------------------------------------
        VBox textBox = new VBox();
        textBox.setAlignment(Pos.CENTER);
        textBox.setSpacing(5);

        // labels
        Label titleLabel = new Label(title.toUpperCase());
        // Changed from white to dark brown so it pops against the gold Canva plate
        titleLabel.setStyle("-fx-text-fill: #3A2303; -fx-font-size: 17px; -fx-font-weight: 900; -fx-letter-spacing: 1px;");
        
        Label subLabel = new Label(subtitle);
        // REMINDER: will change this!!  -----------------------------------------------------
        subLabel.setStyle("-fx-text-fill: #0044aa; -fx-font-size: 14px; -fx-font-weight: bold;");

        textBox.getChildren().addAll(titleLabel, subLabel);
        contentBox.getChildren().addAll(coverView, textBox);

        // REMINDER: will change this!!  -----------------------------------------------------
        cardRoot.getChildren().addAll(frameView, contentBox);

        // mouse hover 
        cardRoot.setOnMouseEntered(e -> {
            cardRoot.setScaleX(1.04);
            cardRoot.setScaleY(1.04);
            cardRoot.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(68,136,255,0.7), 25, 0, 0, 0);");
            cardRoot.requestFocus();
        });
        
        cardRoot.setOnMouseExited(e -> {
            cardRoot.setScaleX(1.0);
            cardRoot.setScaleY(1.0);
            cardRoot.setStyle("");
        });

        cardRoot.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                cardRoot.setScaleX(1.04);
                cardRoot.setScaleY(1.04);
                cardRoot.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(68,136,255,0.7), 25, 0, 0, 0);");
                double hvalue = carouselBox.getChildren().indexOf(cardRoot) / (double) (Math.max(1, carouselBox.getChildren().size() - 1));
                carouselScrollPane.setHvalue(hvalue);
            } else {
                cardRoot.setScaleX(1.0);
                cardRoot.setScaleY(1.0);
                cardRoot.setStyle("");
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