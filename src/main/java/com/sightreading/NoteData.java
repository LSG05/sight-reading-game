package com.sightreading;

public class NoteData {
    public long targetTimeMs;
    public int pixelX;
    public String noteName;

    @Override
    public String toString() {
        return "Note[" + noteName + " @ " + targetTimeMs + "ms, x=" + pixelX + "]";
    }
}