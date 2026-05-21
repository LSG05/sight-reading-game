package com.sightreading;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

// Imports for the dynamic video background
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

public class GameController implements Initializable {

    @FXML private AnchorPane rootPane; // Used to spawn floating text
    @FXML private ImageView sheetMusicView;
    @FXML private Rectangle hitBox;
    @FXML private Rectangle flashOverlay; 
    
    // UI score Elements
    @FXML private Label scoreLabel;
    @FXML private Label comboLabel;

    // End Game Overlay Elements
    @FXML private StackPane resultsOverlay;
    @FXML private Label finalScoreLabel;
    
    // video Background Elements
    @FXML private MediaView bgMediaView;
    private MediaPlayer bgMediaPlayer;

    // gradient objects for the cinematic vignette glow
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

    private int noteIndexInLine = 0;
    private int currentLineIndex = 0;
    private int previousScore = 0; // tracks the score to calculate point additions/deductions

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
            long elapsedMs = (long) audioService.getCurrentTimeMs(); 
            
            checkNoteExpiry(elapsedMs);

            //move index forward once note is processed
            advanceNoteIndex();

            double computedX = computeHitBox(elapsedMs);
            setHitBoxX(computedX);
            updateLineDisplay();

            // End Game Trigger
            int lastLine = songData.lines.size() - 1;
            int lastNote = songData.lines.get(lastLine).notes.size() - 1;
            NoteData veryLastNote = songData.lines.get(lastLine).notes.get(lastNote);
            
            // If the absolute final note has been processed (hit or missed)
            if (currentLineIndex == lastLine && noteIndexInLine == lastNote && veryLastNote.processed) {
                // Add a 1.5-second buffer after the final note so the game doesn't instantly cut off
                if (elapsedMs > veryLastNote.targetTimeMs + 1500) {
                    handleSongFinished();
                }
            }
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String songId = Main.selectedSongId;
        songFolder = "songs/" + songId;
        String songPathString = "/com/sightreading/songs/" + songId + "/audio.wav";
    
        audioService = new AudioService(songPathString, this);

        // VIDEO BG
        try {
            // loads song bg depending on selected song
            String bgFileName = songId + "_bg.mp4"; 
            URL videoUrl = getClass().getResource("/com/sightreading/images/" + bgFileName);
            
            if (videoUrl != null) {
                Media media = new Media(videoUrl.toExternalForm());
                bgMediaPlayer = new MediaPlayer(media);
                bgMediaView.setMediaPlayer(bgMediaPlayer);
                
                // Loop the video continuously
                bgMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                bgMediaPlayer.play();
            } else {
                System.err.println("WARNING: Video background not found: " + bgFileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        songData = NoteLoader.load(songFolder);

        preloadAllImages();

        // 3. Display the first line
        if (!preloadedImages.isEmpty()) {
            sheetMusicView.setImage(preloadedImages.get(0));
        }

        hitBox.setTranslateX(0);

        scoreManager = new ScoreManager(this);
        inputHandler = new InputHandler(this);

        // VIGNETTE
        Stop[] hitStops = new Stop[] {
            new Stop(0.5, Color.TRANSPARENT), 
            new Stop(1.0, Color.web("#4488ff")) 
        };
        hitGradient = new RadialGradient(0, 0, 0.5, 0.5, 0.75, true, CycleMethod.NO_CYCLE, hitStops);

        Stop[] missStops = new Stop[] {
            new Stop(0.5, Color.TRANSPARENT),
            new Stop(1.0, Color.web("#ff4444")) 
        };
        missGradient = new RadialGradient(  0, 0, 0.5, 0.5, 0.75, true, CycleMethod.NO_CYCLE, missStops);
        
        flashOverlay.setEffect(null); 

        // 6. Load Audio
        audioService.loadSong();
        audioService.playSong();
        masterClock.start();
        animationTimer.start();

        // 8. Closing Methods
        // set focus to the sheet music pane so it can receive key events immediately
        Platform.runLater(() -> {
            sheetMusicView.setFocusTraversable(true); 
            sheetMusicView.requestFocus();           
        });
    }

    private void preloadAllImages() {
        for (LineData line : songData.lines) {
            String resourcePath = "/com/sightreading/" + songFolder + "/images/" + line.imageFile;
            URL imageUrl = getClass().getResource(resourcePath);

            if (imageUrl == null) {
                System.err.println("WARNING: Image not found: " + resourcePath);
                continue;
            }

            Image img = new Image(imageUrl.toExternalForm(), false);
            preloadedImages.add(img);
        }
    }

    // VISUAL FEEDBACK
    public void triggerHitFeedback() {
        flashOverlay.setFill(hitGradient); 
        FadeTransition ft = new FadeTransition(Duration.millis(400), flashOverlay);
        ft.setFromValue(0.65); 
        ft.setToValue(0.0);    
        ft.play();
    }

    public void triggerMissFeedback() {
        flashOverlay.setFill(missGradient); 
        FadeTransition ft = new FadeTransition(Duration.millis(400), flashOverlay);
        ft.setFromValue(0.75); 
        ft.setToValue(0.0);
        ft.play();
    }

    // called automatically by ScoreManager when a hit/miss happens
    public void updateUI(int score, int combo, String rating) {
        Platform.runLater(() -> {
            // 1. update the static Hub labels
            scoreLabel.setText("Score: " + score);
            comboLabel.setText("Combo: " + combo);

            // 2. calculate point difference
            int delta = score - previousScore;
            
            // 3. ALWAYS spawn floating text, even if score didn't change 
            spawnFloatingScore(delta, rating);
            
            previousScore = score; // update history for the next hit
        });
    }

    private void spawnFloatingScore(int delta, String rating) {
        // Build the text cleanly
        String pointText = "";
        if (delta > 0) {
            pointText = "+" + delta;
        } else if (delta < 0) {
            pointText = String.valueOf(delta);
        }

        String finalString = rating;
        if (!pointText.isEmpty()) {
            finalString += "\n" + pointText; 
        }

        Label popup = new Label(finalString);
        popup.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        // green/blue for positive, red for misses or negative points
        String color = (rating.equals("MISS") || rating.equals("STRAY") || delta < 0) ? "#ff4444" : "#44ff44"; 
        popup.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        popup.setEffect(new javafx.scene.effect.DropShadow(4, Color.BLACK));
        popup.setMouseTransparent(true); 

        // spawn it directly over the moving hit-box! 
        double spawnX = 200 + hitBox.getTranslateX() - 15; 
        popup.setLayoutX(spawnX);
        popup.setLayoutY(250); 

        rootPane.getChildren().add(popup);

        // animation: float up while fading out
        TranslateTransition floatUp = new TranslateTransition(Duration.millis(500), popup);
        floatUp.setByY(-40); 

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), popup);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        ParallelTransition pt = new ParallelTransition(popup, floatUp, fadeOut);
        pt.setOnFinished(e -> rootPane.getChildren().remove(popup)); 
        pt.play();
    }

    public void swapToLine(int lineIndex) {
        if (lineIndex >= 0 && lineIndex < preloadedImages.size()) {
            sheetMusicView.setImage(preloadedImages.get(lineIndex));
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
            double fakeNextX = 850; 
            double ratio = (double)(elapsedMs - prev.targetTimeMs) / (next.targetTimeMs - prev.targetTimeMs);
            return prev.pixelX + ratio * (fakeNextX - prev.pixelX) - 15;
        }

        // 4. Standard Interpolation
        double ratio = (double)(elapsedMs - prev.targetTimeMs) / (next.targetTimeMs - prev.targetTimeMs);
        return prev.pixelX + ratio * (next.pixelX - prev.pixelX) - 15;
    }

    //new update line index method
    private void advanceNoteIndex() {
        LineData currentLine = songData.lines.get(currentLineIndex);
        NoteData currentNote = currentLine.notes.get(noteIndexInLine);
        long elapsedTime = (long) audioService.getCurrentTimeMs();

        if(currentNote == currentLine.notes.get(currentLine.notes.size() - 1) && currentLineIndex == songData.lines.size() - 1){
            return; 
        }
        
        long nextNoteTime;

        if(noteIndexInLine == 0){
            nextNoteTime = currentLine.notes.get(1).targetTimeMs; 
        } else if (noteIndexInLine < currentLine.notes.size() -1) {
            nextNoteTime = currentLine.notes.get(noteIndexInLine + 1).targetTimeMs;
        } else {
            nextNoteTime = songData.lines.get(currentLineIndex + 1).notes.get(0).targetTimeMs; 
        }

        // index will advance if it is 190 ms before the next note (the tolerance for an okay hit), and if it hasn't already switched 
        // for this note (to prevent multiple advances for the same note due to animationtimer's frequent calls)
        if (elapsedTime >= nextNoteTime - 190 && !currentNote.isSwitched) {
            currentNote.isSwitched = true; 
            if (noteIndexInLine < currentLine.notes.size() - 1) {
                noteIndexInLine++;
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
            triggerMissFeedback(); 
        }
    }

    @FXML
    public void onKeyPressed(KeyEvent event) {
        if (resultsOverlay.isVisible()) return;
        inputHandler.handleKeyPressed(event);
    }
 
    // --- Phase 3 Added Cleanup ---

    /**
     * STOPS all background threads. 
     */
    private void cleanup() {
        if (animationTimer != null) animationTimer.stop();
        if (audioService != null) audioService.stopSong();
        if (masterClock != null) masterClock.stop();
        if (bgMediaPlayer != null) bgMediaPlayer.stop();
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
            Main.setRoot("game");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToSongs() {
        cleanup();
        try {
            Main.setRoot("song-list");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToLeaderboard() {
        cleanup();
        try {
            Main.setRoot("leaderboard");
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
    
        // grab the final score from the ScoreManager
        int finalScore = scoreManager.getScore();
        System.out.println("SONG FINISHED! Final Score: " + finalScore);

        // save to LeaderboardManager
        String pName = (Main.playerName != null && !Main.playerName.isEmpty()) ? Main.playerName : "Guest";
        LeaderboardManager.addScore(pName, finalScore);
        
        // trigger the Overlay Fade-In instead of changing the screen
        Platform.runLater(() -> {
            finalScoreLabel.setText(String.valueOf(finalScore)); // Inject the actual score number
            resultsOverlay.setVisible(true);
            resultsOverlay.setOpacity(0.0);
            
            FadeTransition ft = new FadeTransition(Duration.millis(800), resultsOverlay);
            ft.setToValue(1.0);
            ft.play();
        });
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