package com.sightreading;
import java.net.URL;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;

public class AudioService {
    private MediaPlayer songPlayer;
    private String songPathString;
    public AudioService(String urlAudioString){
        this.songPathString = urlAudioString;
    }
    public void loadSong(){
        try {
            URL musicUrl = getClass().getResource(this.songPathString);
            if (musicUrl != null) {
                Media media = new Media(musicUrl.toExternalForm());
                songPlayer = new MediaPlayer(media);
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
            songPlayer.stop();
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
}