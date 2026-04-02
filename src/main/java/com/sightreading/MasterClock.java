package com.sightreading;

import java.util.concurrent.atomic.AtomicBoolean;

public class MasterClock {
    // Variable for starting the timer
    private long startNano;
    // Boolean to check state of song
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Time methods
    public void start(){
        startNano = System.nanoTime();
        running.set(true);
    }

    public void stop(){
        running.set(false);
    }

    public long getElapsedMs(){
        long endTime = System.nanoTime();
        long elapsedTime = endTime - this.startNano;
        long elapsedTimeMs = elapsedTime/1000000;
        return elapsedTimeMs;
    }

    public boolean isRunning() {return running.get();}

    public long getStartNano() {return this.startNano;} 
}
