package com.example.omronapp.omron;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface for logging Bluetooth operations.
 */
public interface OmronLogger {
    void log(String message);
    List<String> getLogs();
    void clear();

    /**
     * Silent logger for production.
     */
    class Silent implements OmronLogger {
        public void log(String message) { System.out.println(message); }
        public List<String> getLogs() { return new ArrayList<>(); }
        public void clear() {}
    }

    /**
     * Verbose logger for debugging.
     */
    class Verbose implements OmronLogger {
        private final List<String> logs = new ArrayList<>();
        private long startTime = System.currentTimeMillis();

        public void log(String message) {
            long elapsed = System.currentTimeMillis() - startTime;
            String entry = String.format("[%05dms] %s", elapsed, message);
            logs.add(entry);
            System.out.println("DEBUG: " + entry);
        }

        public List<String> getLogs() { return new ArrayList<>(logs); }
        public void clear() { logs.clear(); startTime = System.currentTimeMillis(); }
    }
}
