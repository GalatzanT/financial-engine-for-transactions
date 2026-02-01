import model.Instrument;
import server.TradingEngine;
import server.OrderServer;
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
 * - Pornirea Order Server (TCP pe port 8080)
 * - Crearea și pornirea clienților (TradingBot)
 * - Oprirea sistemului după 1 minut
 */
public class Main {
    // Configurație sistem
    private static final int NUM_INSTRUMENTS = 5;
    private static final int NUM_THREADS = 4;
    private static final int NUM_CLIENTS = 5;
    private static final int RUNTIME_MINUTES = 1;
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "localhost";
    
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
        
        // 3. Pornește Order Server (TCP)
        OrderServer orderServer = new OrderServer(SERVER_PORT, engine);
        orderServer.start();
        
        // Așteaptă 2 secunde ca serverul să fie gata
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println();
        
        // 4. Creează și pornește clienții (boți) - conectare la server TCP
        List<TradingBot> bots = createAndStartBots(SERVER_HOST, SERVER_PORT, instruments);
        System.out.println("✓ Boți porniți: " + bots.size());
        System.out.println();
        
        // 4. Rulează timp de 1 minut
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("Sistem pornit - va rula " + RUNTIME_MINUTES + " minut(e)...");
        System.out.println("Server TCP pe portul " + SERVER_PORT);
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
        
        // Oprește Order Server
        orderServer.stop();
        
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
     * Boții se conectează la server prin TCP socket.
     */
    private static List<TradingBot> createAndStartBots(String serverHost, int serverPort,
                                                       Map<String, Instrument> instruments) {
        List<TradingBot> bots = new ArrayList<>();
        
        for (int i = 1; i <= NUM_CLIENTS; i++) {
            String clientId = "CLIENT-" + i;
            TradingBot bot = new TradingBot(clientId, serverHost, serverPort, instruments);
            bot.start();
            bots.add(bot);
        }
        
        return bots;
    }
}
