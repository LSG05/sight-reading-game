package com.sightreading;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class GameController implements Initializable {

    @FXML private ImageView sheetMusicView;
    @FXML private Rectangle hitBox;
    @FXML private Rectangle flashOverlay; 

    private RadialGradient hitGradient;
    private RadialGradient missGradient;

    private final List<Image> preloadedImages = new ArrayList<>();
    private SongData songData;

    // The song to load. In a later phase this will be passed in
    // from a song selection screen. For now it is hardcoded.
    private String songFolder;
    private AudioService audioService;

    // Declare variables from masterclock and audioservice
    private MasterClock masterClock = new MasterClock();

    // Flattened note list for reading, remove later
    private List<NoteData> songNoteList;

    // declare index variables for iterating lines and notes
    private int noteIndexInLine = 0;
    private int currentLineIndex = 0;

    // ms window for a note to be considered a hit, will be used in input handling
    private final static int MISS_TOLERANCE_MS = 200; 

    // Input and UI handling, add ui controller soon
    private InputHandler inputHandler;

    // Initialize ScoreManager with reference to this GameController for UI updates
    private ScoreManager scoreManager;

    // Declare animationTimer for frame by frame activities
    private AnimationTimer animationTimer = new AnimationTimer() {
        @Override
        public void handle(long now){
            if (!masterClock.isRunning()) return;
            long elapsedMs = (long) audioService.getCurrentTimeMs(); // Use audio service time for synchronization
            
            //check if note is expired
            checkNoteExpiry(elapsedMs);

            //move index forward once note is processed
            advanceNoteIndex();

            double computedX = computeHitBox(elapsedMs);
            setHitBoxX(computedX);
            updateLineDisplay();

            applyParallax(elapsedMs);
        }
    };


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // grab the selected song from song list
        String songId = Main.selectedSongId;
        songFolder = "songs/" + songId;
        String songPathString = "/com/sightreading/songs/" + songId + "/audio.wav";
    
        audioService = new AudioService(songPathString, this);

        // 1. Parse notes.json for the selected song
        songData = NoteLoader.load(songFolder);

        // 2. Pre-load every image for this song into RAM right now
        preloadAllImages();

        // 3. Display the first line
        if (!preloadedImages.isEmpty()) {
            sheetMusicView.setImage(preloadedImages.get(0));
        }

        // 4. Park the hitbox at the left edge
        hitBox.setTranslateX(0);

        System.out.println("Phase 1 complete. Window ready.");

        // initalize score manager and input handler with reference to this gamecontroller for ui updates
        scoreManager = new ScoreManager(this);
        inputHandler = new InputHandler(this);

        // 5. VISUAL FEEDBACK: VIGNETTE
        // blue
        Stop[] hitStops = new Stop[] {
            new Stop(0.5, Color.TRANSPARENT), // center 50% of the screen is transparent
            new Stop(1.0, Color.web("#4488ff")) // fades into solid blue at the edges
        };
        // circular "frame"
        hitGradient = new RadialGradient(0, 0, 0.5, 0.5, 0.75, true, CycleMethod.NO_CYCLE, hitStops);

        // red
        Stop[] missStops = new Stop[] {
            new Stop(0.5, Color.TRANSPARENT),
            new Stop(1.0, Color.web("#ff4444")) 
        };
        missGradient = new RadialGradient(0, 0, 0.5, 0.5, 0.75, true, CycleMethod.NO_CYCLE, missStops);
        
        flashOverlay.setEffect(null); 

        // 6. Load Audio
        audioService.loadSong();

        // 7. Run sequence
        audioService.playSong();
        masterClock.start();
        animationTimer.start();

        // 8. Closing Methods
        // set focus to the sheet music pane so it can receive key events immediately
        Platform.runLater(() -> {
            sheetMusicView.setFocusTraversable(true); 
            sheetMusicView.requestFocus();           
        });

        printActiveThreads();    
    }

    private void printActiveThreads() {
        System.out.println("--- ACTIVE THREADS REPORT ---");
        Thread.getAllStackTraces().keySet().forEach(t -> {
            System.out.println("Thread Name: " + t.getName() + " | Priority: " + t.getPriority() + " | Is Daemon: " + t.isDaemon());
        });
        System.out.println("-----------------------------");
    }

    private void preloadAllImages() {
        for (LineData line : songData.lines) {
            String resourcePath = "/com/sightreading/"
                + songFolder + "/images/" + line.imageFile;

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

    // UPDATED: VISUAL FEEDBACK
    public void triggerHitFeedback() {
        flashOverlay.setFill(hitGradient); // use circular blue gradient
        FadeTransition ft = new FadeTransition(Duration.millis(400), flashOverlay);
        ft.setFromValue(0.50); // glow brightness
        ft.setToValue(0.0);    
        ft.play();
    }

    public void triggerMissFeedback() {
        flashOverlay.setFill(missGradient); // use circular red gradient
        FadeTransition ft = new FadeTransition(Duration.millis(400), flashOverlay);
        ft.setFromValue(0.50); 
        ft.setToValue(0.0);
        ft.play();
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

    public double computeHitBox(long elapsedMs) {
        // 1. Flatten all notes to easily find where we are in time
        List<NoteData> allNotes = new ArrayList<>();
        for (LineData line : songData.lines) {
            allNotes.addAll(line.notes);
        }

        // 2. Find which two notes the current time falls between
        int activeIndex = 0;
        for (int i = 0; i < allNotes.size() - 1; i++) {
            if (elapsedMs >= allNotes.get(i).targetTimeMs && elapsedMs < allNotes.get(i + 1).targetTimeMs) {
                activeIndex = i;
                break;
            }
        }

        // Handle end of song
        if (elapsedMs >= allNotes.get(allNotes.size() - 1).targetTimeMs) {
            return allNotes.get(allNotes.size() - 1).pixelX;
        }

        NoteData prev = allNotes.get(activeIndex);
        NoteData next = allNotes.get(activeIndex + 1);

        // 3. Determine if we are moving to a new line
        // If the next note's X is smaller than the prev note's X, it means it wrapped around to a new line!
        if (next.pixelX < prev.pixelX) {
            // We are in the "gap" between lines. 
            // We calculate a fake target far to the right so it glides off screen.
            double fakeNextX = 850; // slightly off the right edge
            double ratio = (double)(elapsedMs - prev.targetTimeMs) / (next.targetTimeMs - prev.targetTimeMs);
            return prev.pixelX + ratio * (fakeNextX - prev.pixelX) - 50;
        }

        // 4. Standard Interpolation
        double ratio = (double)(elapsedMs - prev.targetTimeMs) / (next.targetTimeMs - prev.targetTimeMs);
        return prev.pixelX + ratio * (next.pixelX - prev.pixelX) - 50;
    }

    //new update line index method
    private void advanceNoteIndex() {
        LineData currentLine = songData.lines.get(currentLineIndex);
        NoteData currentNote = currentLine.notes.get(noteIndexInLine);
        long elapsedTime = (long) audioService.getCurrentTimeMs();

        if(currentNote == currentLine.notes.get(currentLine.notes.size() - 1) && currentLineIndex == songData.lines.size() - 1){
            return; // if we are at the last note of the last line, do not advance further
        }
        
        long nextNoteTime;

        if(noteIndexInLine == 0){
            nextNoteTime = currentLine.notes.get(1).targetTimeMs; // time of second note in current line
        } else if (noteIndexInLine < currentLine.notes.size() -1) {
            nextNoteTime = currentLine.notes.get(noteIndexInLine + 1).targetTimeMs;
        } else {
            nextNoteTime = songData.lines.get(currentLineIndex + 1).notes.get(0).targetTimeMs; // time of first note in next line
        }
            
        System.out.println("nextNoteTime: " + nextNoteTime + "ms, elapsedTime: " + elapsedTime + "ms");

        // index will advance if it is 190 ms before the next note (the tolerance for an okay hit), and if it hasn't already switched 
        // for this note (to prevent multiple advances for the same note due to animationtimer's frequent calls)
        if (elapsedTime >= nextNoteTime - 190 && !currentNote.isSwitched) {
            currentNote.isSwitched = true; // add a flag to prevent multiple switches for the same note
            if (noteIndexInLine < currentLine.notes.size() - 1) {
                noteIndexInLine++;
                System.out.println("Advanced to note index " + noteIndexInLine + " in line " + currentLineIndex + " (elapsed: " + elapsedTime + "ms)");
            } else {
                noteIndexInLine = 0;
                currentLineIndex++;
            }
        }
    }

    private void updateLineDisplay() {
        long elapsedMs = (long) audioService.getCurrentTimeMs();
        LineData lineData = songData.lines.get(currentLineIndex);
        NoteData lastNoteOfLine = lineData.notes.get(lineData.notes.size() - 1);

        if (currentLineIndex > 0) {
            LineData prevLine = songData.lines.get(currentLineIndex - 1);
            NoteData lastNoteOfPrevLine = prevLine.notes.get(prevLine.notes.size() - 1);

            if (elapsedMs < lastNoteOfPrevLine.targetTimeMs + 200) {
                swapToLine(currentLineIndex - 1);
                return;
            }
        }
        swapToLine(currentLineIndex);
    }

    private void checkNoteExpiry(long elapsedMS) {
        LineData currentLine = songData.lines.get(currentLineIndex);
        NoteData currentNote = currentLine.notes.get(noteIndexInLine);

        //If clock has passed the note's target time by more than the miss tolerance, mark it as missed and move on
        if (!currentNote.processed && elapsedMS > (currentNote.targetTimeMs + MISS_TOLERANCE_MS)) {
            currentNote.processed = true;
            currentNote.isHit = false; 
            scoreManager.registerMiss();
            triggerMissFeedback(); // Trigger visual red flash for an expired note!
            System.out.println("Note EXPIRED and missed: " + currentNote.noteName + " (elapsed: " + elapsedMS + "ms, target: " + currentNote.targetTimeMs + "ms)");
        }
    }

    public void updateUI(int score, int combo, String rating) {
        //printing to console for now since this is for UI
        System.out.println("UI Update -> Score: " + score + " | Combo: " + combo + " | Rating: " + rating);
    }

    private void applyParallax(long elapsedMs) {
        // Optional: Implement parallax effect on sheet music based on elapsed time
        // This is a placeholder for where you would add code to adjust the position of the sheet music
        // to create a parallax scrolling effect as the song progresses.
    }

    // handles keyboard press events
    @FXML
    public void onKeyPressed(KeyEvent event) {
        inputHandler.handleKeyPressed(event);
    }
 
    // --- Phase 3 Added Cleanup ---

    /**
     * STOPS all background threads. 
     */
    private void cleanup() {
        System.out.println("Cleaning up game threads...");
        if (animationTimer != null) {
            animationTimer.stop();
        }
        if (audioService != null) {
            audioService.stopSong();
        }
        if (masterClock != null) {
            masterClock.stop();
        }
    }

    @FXML
    private void handleExit() {
        cleanup();
        try {
            Main.setRoot("home");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRestart() {
        cleanup();
        try {
            // To restart perfectly, reload the FXML scene from scratch
            Main.setRoot("game");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // handle song finished
    public void handleSongFinished(){
        // Stop all timers/clocks
        audioService.stopSong();
        masterClock.stop();
        animationTimer.stop();
    
        // TODO LATER: show results of screen and add to leaderboard
    }

    // Add these to GameController.java so InputHandler can see them
    public NoteData getCurrentNote() {
        return songData.lines.get(currentLineIndex).notes.get(noteIndexInLine);
    }


    public MasterClock getMasterClock() {
        return this.masterClock;
    }


    public ScoreManager getScoreManager() {
        return this.scoreManager;
    }

    // Added getter for AudioService to allow InputHandler to access current time for timing error calculations
    public AudioService getAudioService() {
        return this.audioService;
    }

    // Add reset function for song states
    public void resetSongStates(){
        // Reset the indices
        this.noteIndexInLine = 0;
        this.currentLineIndex = 0;

        // Loop through the data structure to clear flags
        for (LineData line : songData.lines) {
            for (NoteData note : line.notes) {
                note.processed = false;
                note.isHit = false;
                note.isSwitched = false;
            }
        }
    }
}