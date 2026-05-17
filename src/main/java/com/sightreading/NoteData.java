package com.sightreading;

public class NoteData {
    public long targetTimeMs;
    public int pixelX;
    public String noteName;
    public boolean processed = false;
    public boolean isHit = false;
    public boolean isSwitched = false; // added isSeitched to track advancement of note index

    @Override
    public String toString() {
        return "Note[" + noteName + " @ " + targetTimeMs + "ms, x=" + pixelX + "]";
    }
}