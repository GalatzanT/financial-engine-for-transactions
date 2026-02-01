import model.Instrument;
import server.TradingEngine;
import client.TradingBot;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Clasa principală pentru pornirea sistemului Financial Engine.
 * 
 * Orchestrează:
 * - Crearea instrumentelor financiare
 * - Pornirea Trading Engine
 * - Crearea și pornirea clienților (TradingBot)
 * - Oprirea sistemului după 3 minute
 */
public class Main {
    // Configurație sistem
    private static final int NUM_INSTRUMENTS = 5;
    private static final int NUM_THREADS = 4;
    private static final int NUM_CLIENTS = 5;
    private static final int RUNTIME_MINUTES = 1;
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════╗");
        System.out.println("║   FINANCIAL ENGINE - Sistem Execuție Ordine       ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.println();
        
        // 1. Creează instrumentele financiare
        Map<String, Instrument> instruments = createInstruments();
        System.out.println("✓ Instrumente create: " + instruments.size());
        for (Instrument instrument : instruments.values()) {
            System.out.println("  " + instrument);
        }
        System.out.println();
        
        // 2. Creează și pornește Trading Engine
        TradingEngine engine = new TradingEngine(instruments, NUM_THREADS);
        engine.start();
        System.out.println();
        
        // 3. Creează și pornește clienții (boți)
        List<TradingBot> bots = createAndStartBots(engine, instruments);
        System.out.println("✓ Boți porniți: " + bots.size());
        System.out.println();
        
        // 4. Rulează timp de 3 minute
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("Sistem pornit - va rula " + RUNTIME_MINUTES + " minute...");
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println();
        
        try {
            TimeUnit.MINUTES.sleep(RUNTIME_MINUTES);
        } catch (InterruptedException e) {
            System.err.println("Întrerupere: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        
        // 5. Oprește sistemul
        System.out.println("\n═══════════════════════════════════════════════════");
        System.out.println("Timp expirat - oprire sistem...");
        System.out.println("═══════════════════════════════════════════════════");
        
        // Oprește boții
        for (TradingBot bot : bots) {
            bot.stop();
        }
        System.out.println("✓ Boți opriți");
        
        // Oprește engine-ul
        engine.shutdown();
        
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║   Sistem oprit cu succes!                         ║");
        System.out.println("║   Verifică fișierele pentru logs:                 ║");
        System.out.println("║   - orders.txt                                    ║");
        System.out.println("║   - executions.txt                                ║");
        System.out.println("║   - cancellations.txt                             ║");
        System.out.println("║   - audit_log.txt                                 ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
    }
    
    /**
     * Creează instrumentele financiare pentru sistem.
     */
    private static Map<String, Instrument> createInstruments() {
        Map<String, Instrument> instruments = new HashMap<>();
        
        // Instrumente cu parametri diferiți
        instruments.put("AAPL", new Instrument(
            "AAPL",          // id
            150.0,           // preț inițial
            1000.0,          // lichiditate maximă
            2.0,             // volatilitate (sigma)
            0.1              // trend (mu)
        ));
        
        instruments.put("GOOGL", new Instrument(
            "GOOGL",
            2800.0,
            800.0,
            3.5,
            0.15
        ));
        
        instruments.put("MSFT", new Instrument(
            "MSFT",
            330.0,
            1200.0,
            1.5,
            0.05
        ));
        
        instruments.put("TSLA", new Instrument(
            "TSLA",
            900.0,
            600.0,
            5.0,
            0.2
        ));
        
        instruments.put("AMZN", new Instrument(
            "AMZN",
            3300.0,
            900.0,
            2.5,
            0.1
        ));
        
        return instruments;
    }
    
    /**
     * Creează și pornește boții de tranzacționare.
     */
    private static List<TradingBot> createAndStartBots(TradingEngine engine, 
                                                       Map<String, Instrument> instruments) {
        List<TradingBot> bots = new ArrayList<>();
        
        for (int i = 1; i <= NUM_CLIENTS; i++) {
            String clientId = "CLIENT-" + i;
            TradingBot bot = new TradingBot(clientId, engine, instruments);
            bot.start();
            bots.add(bot);
        }
        
        return bots;
    }
}
