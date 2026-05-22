package com.sightreading;
import java.net.URL;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;

public class AudioService {
    private MediaPlayer songPlayer;
    private String songPathString;
    private GameController gameController;
    public AudioService(String urlAudioString, GameController gameController){
        this.songPathString = urlAudioString;
        this.gameController = gameController;
    }
    public void loadSong(){
        try {
            URL musicUrl = getClass().getResource(this.songPathString);
            if (musicUrl != null) {
                Media media = new Media(musicUrl.toExternalForm());
                songPlayer = new MediaPlayer(media);

                // attach listener for when song ends
                songPlayer.setOnEndOfMedia(() -> {
                    System.out.println("Audio Thread: Song has physically finished.");
                    // Use Platform.runLater because the MediaPlayer thread is NOT the UI thread
                    Platform.runLater(() -> {
                        // trigger end of song actions in the GameController
                        gameController.handleSongFinished();
                    });
                });

            } else {
                System.err.println("Error: Music file not found at " + this.songPathString);
            }
        } catch (Exception e) {
            System.err.println("Error loading music: " + e.getMessage());
        }
    }
    public void playSong(){
        if (songPlayer != null) {
            songPlayer.seek(javafx.util.Duration.ZERO);
            songPlayer.play();
        }
    }
    public void stopSong(){ 
        if (songPlayer != null) {
            if (songPlayer.getStatus() != Status.STOPPED && songPlayer.getStatus() != Status.DISPOSED) {
                try {
                    songPlayer.stop();
                } catch (Exception e) {
                }
        } 
        }
        dispose();
    }
    public boolean isPlaying(){
        if (songPlayer != null && songPlayer.getStatus().equals(Status.PLAYING)){
            return true;
        } else{
            return false;
        }
    }
    public void dispose(){
        if (songPlayer != null) {
        songPlayer.dispose();
        songPlayer = null;
        }
    }

    // future note: pause and resume

    // Added method to get current time of the song for timing error calculations in InputHandler
    public double getCurrentTimeMs(){
        if (songPlayer != null && songPlayer.getStatus() == Status.PLAYING) {
            return songPlayer.getCurrentTime().toMillis();
        } 
        return 0.0;
    }

    // Add for hover
    public void playPreview(double seconds) {
    if (songPlayer != null) {
        songPlayer.setOnReady(() -> {
            // Avoid game logic triggers by clearing listener
            songPlayer.setOnEndOfMedia(null); 
            
            // Set preview song duration
            songPlayer.seek(javafx.util.Duration.seconds(3)); 
            songPlayer.setStopTime(javafx.util.Duration.seconds(3 + seconds));
            
            // Debug print
            songPlayer.setOnStopped(() -> System.out.println("Preview finished"));

            // Play Audio
            songPlayer.play();
        });
        
        // Error handling: If it fails to reach "Ready"
        songPlayer.setOnError(() -> System.err.println("Media error: " + songPlayer.getError()));
    }
}

}