package com.sightreading;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class GameController implements Initializable {

    @FXML private ImageView sheetMusicView;
    @FXML private Rectangle hitBox;

    private final List<Image> preloadedImages = new ArrayList<>();
    private SongData songData;

    // The song to load. In a later phase this will be passed in
    // from a song selection screen. For now it is hardcoded.
    private static final String SONG_FOLDER = "songs/twinkle";

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // 1. Parse notes.json for the selected song
        songData = NoteLoader.load(SONG_FOLDER);

        // 2. Pre-load every image for this song into RAM right now
        preloadAllImages();

        // 3. Display the first line
        if (!preloadedImages.isEmpty()) {
            sheetMusicView.setImage(preloadedImages.get(0));
        }

        // 4. Park the hitbox at the left edge
        hitBox.setTranslateX(0);

        System.out.println("Phase 1 complete. Window ready.");
    }

    private void preloadAllImages() {
        for (LineData line : songData.lines) {
            String resourcePath = "/com/sightreading/"
                + SONG_FOLDER + "/images/" + line.imageFile;

            URL imageUrl = getClass().getResource(resourcePath);

            if (imageUrl == null) {
                System.err.println("WARNING: Image not found: " + resourcePath);
                continue;
            }

            // false = load immediately, not lazily in the background
            Image img = new Image(imageUrl.toExternalForm(), false);
            preloadedImages.add(img);
            System.out.println("Pre-loaded: " + line.imageFile);
        }

        System.out.println("Done. " + preloadedImages.size() + " images in RAM.");
    }

    // --- Stubs for Phase 2 ---

    public void swapToLine(int lineIndex) {
        if (lineIndex >= 0 && lineIndex < preloadedImages.size()) {
            sheetMusicView.setImage(preloadedImages.get(lineIndex));
        }
    }

    public void setHitBoxX(double x) {
        hitBox.setTranslateX(x);
    }
}