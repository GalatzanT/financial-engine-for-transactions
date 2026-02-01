package util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger simplu pentru scriere în fișiere text.
 * Thread-safe prin sincronizare.
 */
public class FileLogger {
    private static final DateTimeFormatter TIME_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Scrie un mesaj într-un fișier (sincronizat).
     * 
     * @param filename Numele fișierului
     * @param message Mesajul de scris
     */
    public synchronized static void log(String filename, String message) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(filename, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Eroare la scrierea în fișier " + filename + ": " + e.getMessage());
        }
    }
    
    /**
     * Scrie un mesaj cu timestamp într-un fișier.
     */
    public synchronized static void logWithTimestamp(String filename, String message) {
        String timestampedMessage = LocalDateTime.now().format(TIME_FORMAT) + " | " + message;
        log(filename, timestampedMessage);
    }
    
    /**
     * Creează sau suprascrie un fișier cu un header.
     */
    public synchronized static void initializeFile(String filename, String header) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, false))) {
            writer.write(header);
            writer.newLine();
            writer.write("=".repeat(80));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Eroare la inițializarea fișierului " + filename + ": " + e.getMessage());
        }
    }
}
