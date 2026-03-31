package com.sightreading;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class NoteLoader {

    /**
     * Loads the notes.json from a specific song folder.
     *
     * @param songFolder e.g. "songs/twinkle"
     * @return A populated SongData object containing all lines and notes.
     */
    public static SongData load(String songFolder) {
        Gson gson = new Gson();

        String path = "/com/sightreading/" + songFolder + "/notes.json";

        InputStream stream = NoteLoader.class.getResourceAsStream(path);

        Objects.requireNonNull(stream,
            "notes.json not found at: " + path
            + "\nCheck that the file exists in src/main/resources" + path);

        SongData song = gson.fromJson(new InputStreamReader(stream), SongData.class);

        // Debug output so you can verify the parse
        System.out.println("=== SONG LOADED: " + songFolder + " ===");
        System.out.println("Total lines: " + song.lines.size());
        for (LineData line : song.lines) {
            System.out.println("  Line " + line.lineIndex
                + " | " + line.imageFile
                + " | " + line.notes.size() + " notes");
            for (NoteData note : line.notes) {
                System.out.println("    " + note);
            }
        }
        System.out.println("==============================");

        return song;
    }
}