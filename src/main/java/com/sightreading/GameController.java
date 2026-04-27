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
import javafx.scene.input.KeyEvent;
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

    // Flattened note list for reading, remove later
    private List<NoteData> songNoteList;

    // declare index variables for iterating lines and notes
    private int noteIndexInLine = 0;
    private int currentLineIndex = 0;

    // ms window for a note to be considered a hit, will be used in input handling
    private final static int MISS_TOLERANCE_MS = 200; 

    // Input and UI handling, add ui controller soon
    private InputHandler inputHandler;

    // Declare animationTimer for frame by frame activities
    private AnimationTimer animationTimer = new AnimationTimer() {
        @Override
        public void handle(long now){
            if (!masterClock.isRunning()) return;
            long elapsedMs = masterClock.getElapsedMs();
            double computedX = computeHitBox(elapsedMs);
            setHitBoxX(computedX);
            updateLineDisplay();
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

        // // 5. Create noteslist for easy access of notes from json file, will remove due to redundancy
        // songNoteList = new ArrayList<>();
        // for(LineData line: songData.lines){
        //     for(NoteData note: line.notes){
        //         if(note != null){
        //             songNoteList.add(note);
        //         }
        //     }
        // }

        // 6. Load Audio
        audioService.loadSong();

        // 7. Run sequence
        audioService.playSong();
        masterClock.start();
        animationTimer.start();

        // 8. Closing Methods
        if(currentLineIndex >= songData.lines.size()){
            audioService.stopSong();
            masterClock.stop();
            animationTimer.stop();
        }
        

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
            System.out.println("Swapped to line " + lineIndex);
        }
    }

    public void setHitBoxX(double x) {
        hitBox.setTranslateX(x);
    }

    public double computeHitBox(long elapsedMs){
        // Obtain current line and note list for the line
        LineData currentLine = songData.lines.get(currentLineIndex);
        List<NoteData> notes = currentLine.notes;

        // Scope handling of out of bounds
        // if (noteIndexInLine >= notes.size() - 1) {
        //     return notes.get(notes.size() - 1).pixelX;
        // }
        if (currentLineIndex >= songData.lines.size() - 1 && 
            noteIndexInLine >= notes.size() - 1) {
            return notes.get(notes.size() - 1).pixelX;
        }

        // // Reset noteindex after done with line, iterate line
        // if (noteIndexInLine >= notes.size() && currentLineIndex < songData.lines.size() - 1) {
        //     currentLineIndex++;
        //     noteIndexInLine = 0;
        // }

        // Reset noteindex after done with line, iterate line
        if (noteIndexInLine >= notes.size() - 1 && currentLineIndex < songData.lines.size() - 1) {
            currentLineIndex++;
            noteIndexInLine = 0;
        }

        // Compute position of hitbox
        long prevNoteTime = notes.get(noteIndexInLine).targetTimeMs;
        long nextNoteTime = notes.get(noteIndexInLine + 1).targetTimeMs;
        double noteXPrevDist = notes.get(noteIndexInLine).pixelX;
        double noteXNextDist = notes.get(noteIndexInLine + 1).pixelX;
    }

    //new update line index method
    private void advanceNoteIndex() {
        LineData currentLine = songData.lines.get(currentLineIndex);
        NoteData currentNote = currentLine.notes.get(noteIndexInLine);

        // Advance only if current note is processed
        if (currentNote.processed) {
            if (noteIndexInLine < currentLine.notes.size() -1) {
                noteIndexInLine++;
            } else if (currentLineIndex < songData.lines.size() -1) {
                currentLineIndex++;
                noteIndexInLine = 0;
            }
        } 
        
    }

    private void updateLineDisplay() {
        if (currentLineIndex >= 0 && currentLineIndex < songData.lines.size()) {
            swapToLine(this.currentLineIndex);
        }
    }

    private void checkNoteExpiry(long elapsedMS) {
        LineData currentLine = songData.lines.get(currentLineIndex);
        NoteData currentNote = currentLine.notes.get(noteIndexInLine);

        //If clock has passed the note's target time by more than the miss tolerance, mark it as missed and move on
        if (!currentNote.processed && elapsedMS > (currentNote.targetTimeMs + MISS_TOLERANCE_MS)) {
            currentNote.processed = true;
            currentNote.isHit = false; 
            ScoreManager.registerMiss();
            System.out.println("Note EXPIRED and missed: " + currentNote.noteName);
        }
    }

    // handles keyboard press events
    @FXML
    public void onKeyPressed(KeyEvent event) {
        inputHandler.handleKeyPressed(event);
    }
 
}

// stop methods, make sure box is on top, implement separate thread for audioservice, edge cases and bounds + address delay, switch images