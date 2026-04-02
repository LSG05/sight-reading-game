package com.sightreading;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

public class GameController implements Initializable {

    @FXML private ImageView sheetMusicView;
    @FXML private Rectangle hitBox;

    private final List<Image> preloadedImages = new ArrayList<>();
    private SongData songData;

    // The song to load. In a later phase this will be passed in
    // from a song selection screen. For now it is hardcoded.
    private static final String SONG_FOLDER = "songs/twinkle";
    private static String SONG_PATH_STRING = "/com/sightreading/songs/twinkle/audio.wav"; // hard code temporary

    // Declare variables from masterclock and audioservice
    private MasterClock masterClock = new MasterClock();
    private AudioService audioService = new AudioService(SONG_PATH_STRING);

    // Flattened note list for reading
    private List<NoteData> songNoteList;

    //
    private int noteIndex = 0;

    // Declare animationTimer for frame by frame activities
    private AnimationTimer animationTimer = new AnimationTimer() {
        @Override
        public void handle(long now){
            if (!masterClock.isRunning()) return;
            long elapsedMs = masterClock.getElapsedMs();
            double computedX = computeHitBox(elapsedMs);
            setHitBoxX(computedX);
        }
    };


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

        // 5. Create noteslist for easy access of notes from json file
        songNoteList = new ArrayList<>();
        for(LineData line: songData.lines){
            for(NoteData note: line.notes){
                if(note != null){
                    songNoteList.add(note);
                }
            }
        }

        // 6. Load Audio
        audioService.loadSong();

        // 7. Run sequence
        audioService.playSong();
        masterClock.start();
        animationTimer.start();

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

    public double computeHitBox(long elapsedMs){
        if(songNoteList.size() == 0){return 0;}
        int index = this.noteIndex;
        long prevNoteTime = songNoteList.get(index).targetTimeMs;
        long nextNoteTime = songNoteList.get(index + 1).targetTimeMs;
        long noteXPrevDist = songNoteList.get(index).pixelX;
        long noteXNextDist = songNoteList.get(index + 1).pixelX;
        double hitBoxPosition = noteXPrevDist + ((double)(elapsedMs-prevNoteTime)/(nextNoteTime-prevNoteTime))*(noteXNextDist - noteXPrevDist);
        if (elapsedMs >= nextNoteTime && noteIndex < songNoteList.size() - 1) {
            noteIndex++;
        }
        return hitBoxPosition;
    }
}

// stop methods, make sure box is on top, implement separate thread for audioservice, edge cases and bounds + address delay, switch images