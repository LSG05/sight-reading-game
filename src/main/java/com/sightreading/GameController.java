package com.sightreading;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.PauseTransition;
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
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class GameController implements Initializable {
    // Ui layout elements
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
    
    // Static Background Image & Particles
    @FXML private ImageView bgImageView;
    @FXML private Pane particlePane;
    private List<Particle> particles = new ArrayList<>();
    private AnimationTimer particleTimer;
    private Random random = new Random();

    // Interactive Keyboard Elements
    @FXML private ImageView btnC, btnD, btnE, btnF, btnG, btnA, btnB;

    // Gradient objects for the cinematic vignette glow
    private RadialGradient hitGradient;
    private RadialGradient missGradient;

    private final List<Image> preloadedImages = new ArrayList<>();
    private SongData songData;

    // The song to load. In a later phase this will be passed in
    // from a song selection screen. For now it is hardcoded.
    private String songFolder;
    private AudioService audioService;

    private int noteIndexInLine = 0;
    private int currentLineIndex = 0;
    private int previousScore = 0; // tracks the score to calculate point additions/deductions

    private final static int MISS_TOLERANCE_MS = 200; 
    private final static double SCALE_FACTOR = 1.5; //scale factor to adjust image size

    // Input and UI handling
    private InputHandler inputHandler;

    // Initialize ScoreManager with reference to this GameController for UI updates
    private ScoreManager scoreManager;

    // Declare animationTimer for frame by frame activities
    private AnimationTimer animationTimer = new AnimationTimer() {
        @Override
        public void handle(long now){
            long elapsedMs = (long) audioService.getCurrentTimeMs(); 
            
            checkNoteExpiry(elapsedMs);

            // Move index forward once note is processed
            advanceNoteIndex();

            double computedX = computeHitBox(elapsedMs);
            setHitBoxX(computedX);
            updateLineDisplay();

            // End game Trigger
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

        // Image background
        try {
            // Loads song background depending on selected song
            String bgFileName = songId + "_bg.jpg"; 
            URL imageUrl = getClass().getResource("/com/sightreading/images/" + bgFileName);
            
            if (imageUrl != null) {
                bgImageView.setImage(new Image(imageUrl.toExternalForm()));
            } else {
                System.err.println("WARNING: Image background not found: " + bgFileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Particles
        createParticles();

        songData = NoteLoader.load(songFolder);

        preloadAllImages();

        // Display the first line
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

        // Load Audio
        audioService.loadSong();
        audioService.playSong();
        animationTimer.start();

        // Set focus to the sheet music pane so it can receive key events immediately
        Platform.runLater(() -> {
            sheetMusicView.setFocusTraversable(true); 
            sheetMusicView.requestFocus();           
        });
    }

    // FLOATING PARTICLES
    private void createParticles() {
        for (int i = 0; i < 60; i++) {
            double radius = random.nextDouble() * 2 + 1;
            Circle circle = new Circle(radius, Color.web("#ffffff")); 
            circle.setEffect(new DropShadow(10, Color.web("#ffffff")));
            
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

    // Called automatically by ScoreManager when a hit/miss happens
    public void updateUI(int score, int combo, String rating) {
        Platform.runLater(() -> {
            // Update the static hub labels
            scoreLabel.setText("Score: " + score);
            comboLabel.setText("Combo: " + combo);

            // Calculate point difference
            int delta = score - previousScore;
            
            // Always spawn floating text, even if score didn't change 
            spawnFloatingScore(delta, rating);
            
            previousScore = score; // Update history for the next hit
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
        
        // Green/blue for positive, red for misses or negative points
        String color = (rating.equals("MISS") || rating.equals("STRAY") || delta < 0) ? "#ff4444" : "#44ff44"; 
        popup.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        popup.setEffect(new javafx.scene.effect.DropShadow(4, Color.BLACK));
        popup.setMouseTransparent(true); 

        // Spawn it directly over the moving hitbox
        double spawnX = hitBox.getTranslateX() - 22.5; // UPDATE: removed 200 and turned -15 to -22.5 as resized images start at x=0
        popup.setLayoutX(spawnX);
        popup.setLayoutY(250); 

        rootPane.getChildren().add(popup);

        // Animation: float up while fading out
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
        // Flatten all notes to easily find where we are in time
        List<NoteData> allNotes = new ArrayList<>();
        for (LineData line : songData.lines) {
            allNotes.addAll(line.notes);
        }

        // Find which two notes the current time falls between
        int activeIndex = 0;
        for (int i = 0; i < allNotes.size() - 1; i++) {
            if (elapsedMs >= allNotes.get(i).targetTimeMs && elapsedMs < allNotes.get(i + 1).targetTimeMs) {
                activeIndex = i;
                break;
            }
        }

        // Handle end of song
        if (elapsedMs >= allNotes.get(allNotes.size() - 1).targetTimeMs) {
            return allNotes.get(allNotes.size() - 1).pixelX * SCALE_FACTOR; // Apply scale factor to adjust for image size changes
        }

        NoteData prev = allNotes.get(activeIndex);
        NoteData next = allNotes.get(activeIndex + 1);
        // Applying scale factor to adjust for image size changes
        double prevX = prev.pixelX * SCALE_FACTOR; 
        double nextX = next.pixelX * SCALE_FACTOR;


        // Determine if we are moving to a new line
        // If the next note's X is smaller than the prev note's X, it means it wrapped around to a new line
        if (next.pixelX < prev.pixelX) {
            double fakeNextX = 850 * SCALE_FACTOR; // Apply scale factor to adjust for image size changes
            double ratio = (double)(elapsedMs - prev.targetTimeMs) / (next.targetTimeMs - prev.targetTimeMs);
            return prevX + ratio * (fakeNextX - prevX) - 22.5; // Used scaled prevX and fakeNextX
        }

        // Standard Interpolation
        double ratio = (double)(elapsedMs - prev.targetTimeMs) / (next.targetTimeMs - prev.targetTimeMs);
        return prevX + ratio * (nextX - prevX) - 22.5; // Used scaled prevX and nextX
    }

    // Update line index methods
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

        // Index will advance if it is 190 ms before the next note (the tolerance for an okay hit), and if it hasn't already switched 
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

        // If clock has passed the note's target time by more than the miss tolerance, mark it as missed and move on
        if (!currentNote.processed && elapsedMS > (currentNote.targetTimeMs + MISS_TOLERANCE_MS)) {
            currentNote.processed = true;
            currentNote.isHit = false; 
            scoreManager.registerMiss();
            triggerMissFeedback(); 
        }
    }

    // KEYBOARD LETTER BUTTONS
    private void triggerKeyGlow(ImageView keyView, Color glowColor) {
        if (keyView == null) return;
        
        DropShadow glow = new DropShadow(30, glowColor);
        glow.setSpread(0.6);
        keyView.setEffect(glow);
        keyView.setTranslateY(5); // Simulate a physical button getting pushed down

        // Turn the glow off quickly like a real piano key
        PauseTransition pause = new PauseTransition(Duration.millis(150));
        pause.setOnFinished(e -> {
            keyView.setEffect(null);
            keyView.setTranslateY(0);
        });
        pause.play();
    }

    @FXML
    public void onKeyPressed(KeyEvent event) {
        if (resultsOverlay.isVisible()) return;

        // Visual feedback for the letter buttons
        switch (event.getCode()) {
            case C: triggerKeyGlow(btnC, Color.web("#3866b3")); break; // Blue
            case D: triggerKeyGlow(btnD, Color.web("#8e46b3")); break; // Purple
            case E: triggerKeyGlow(btnE, Color.web("#e883ab")); break; // Pink
            case F: triggerKeyGlow(btnF, Color.web("#eb8731")); break; // Orange
            case G: triggerKeyGlow(btnG, Color.web("#bf291e")); break; // Red
            case A: triggerKeyGlow(btnA, Color.web("#5db069")); break; // Green
            case B: triggerKeyGlow(btnB, Color.web("#f4d610")); break; // Yellow
            default: break; 
        }

        // Pass the input to existing game logic
        inputHandler.handleKeyPressed(event);
    }
 
    // Cleanup
    // Stops all background threads

    private void cleanup() {
        if (animationTimer != null) animationTimer.stop();
        if (audioService != null) audioService.stopSong();
        if (particleTimer != null) particleTimer.stop();
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

    // Handle song finished
    public void handleSongFinished(){
        // Stop all timers/clocks
        audioService.stopSong();
        animationTimer.stop();
    
        // Grab the final score from the ScoreManager
        int finalScore = scoreManager.getScore();
        System.out.println("SONG FINISHED! Final Score: " + finalScore);

        // Save to LeaderboardManager
        String pName = (Main.playerName != null && !Main.playerName.isEmpty()) ? Main.playerName : "Guest";
        LeaderboardManager.addScore(pName, finalScore);
        
        // Trigger the Overlay Fade-In instead of changing the screen
        Platform.runLater(() -> {
            finalScoreLabel.setText(String.valueOf(finalScore)); // Inject the actual score number
            resultsOverlay.setVisible(true);
            resultsOverlay.setOpacity(0.0);
            
            FadeTransition ft = new FadeTransition(Duration.millis(800), resultsOverlay);
            ft.setToValue(1.0);
            ft.play();
        });
    }

    // Visibility of current note for InputHandler 
    public NoteData getCurrentNote() {
        return songData.lines.get(currentLineIndex).notes.get(noteIndexInLine);
    }

    public ScoreManager getScoreManager() {
        return this.scoreManager;
    }

    // Getter for AudioService to allow InputHandler to access current time for timing error calculations
    public AudioService getAudioService() {
        return this.audioService;
    }

    // Reset function for song states
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