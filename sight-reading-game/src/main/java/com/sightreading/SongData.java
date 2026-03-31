package com.sightreading;

import java.util.List;

public class SongData {
    public String id;
    public String title;
    public String composer;
    public int bpm;
    public String folder;        // e.g. "songs/twinkle"
    public List<LineData> lines; // populated by NoteLoader after reading notes.json
}