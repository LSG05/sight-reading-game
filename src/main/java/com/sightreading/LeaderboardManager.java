package com.sightreading;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardManager {
    private static final String FILE_PATH = "leaderboard.json";
    private static final Gson gson = new Gson();

    public static List<LeaderboardEntry> getScores() {
        try (Reader reader = new FileReader(FILE_PATH)) {
            Type listType = new TypeToken<ArrayList<LeaderboardEntry>>(){}.getType();
            List<LeaderboardEntry> scores = gson.fromJson(reader, listType);
            
            if (scores == null) return new ArrayList<>();
            
            scores.sort((a, b) -> Integer.compare(b.score, a.score));
            return scores;
            
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void addScore(String playerName, int score) {
        List<LeaderboardEntry> scores = getScores();
        
        boolean playerExists = false;

        // loop through entries to see if this player already has a score saved
        for (LeaderboardEntry entry : scores) {
            // Ignore case (ex: "myles" == "Myles" == "MYLES")
            if (entry.playerName.equalsIgnoreCase(playerName)) {
                playerExists = true;
                
                // only overwrite score if the new score is HIGHER
                if (score > entry.score) {
                    entry.score = score;
                    entry.playerName = playerName; 
                }
                break; 
            }
        }
        
        // if not on the list, add user as new entry
        if (!playerExists) {
            scores.add(new LeaderboardEntry(playerName, score));
        }
        
        try (Writer writer = new FileWriter(FILE_PATH)) {
            gson.toJson(scores, writer);
        } catch (IOException e) {
            System.err.println("Failed to save score: " + e.getMessage());
        }
    }
}