package client;

import model.*;
import java.util.Random;
import java.util.Map;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;

/**
 * Simulator de client (bot de tranzacționare).
 * Comunică cu serverul prin socket TCP (port 8080).
 */
public class TradingBot implements Runnable {
    private final String clientId;
    private final String serverHost;
    private final int serverPort;
    private final Map<String, Instrument> instruments;
    private final Random random;
    private final ScheduledExecutorService scheduler;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = false;
    
    /**
     * Constructor pentru bot.
     * 
     * @param clientId ID-ul clientului
     * @param serverHost Adresa serverului (ex: localhost)
     * @param serverPort Portul serverului (ex: 8080)
     * @param instruments Map cu instrumentele disponibile
     */
    public TradingBot(String clientId, String serverHost, int serverPort,
                     Map<String, Instrument> instruments) {
        this.clientId = clientId;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.instruments = instruments;
        this.random = new Random();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    /**
     * Conectează botul la server.
     */
    private boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return true;
        } catch (IOException e) {
            System.err.println("Eroare conectare bot " + clientId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Deconectează botul de la server.
     */
    private void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // Ignoră erori la închidere
        }
    }
    
    /**
     * Pornește botul (trimite ordine la fiecare 1 secundă).
     */
    public void start() {
        // Conectează la server
        if (!connect()) {
            System.err.println("Bot " + clientId + " nu s-a putut conecta la server!");
            return;
        }
        
        running = true;
        scheduler.scheduleAtFixedRate(
            this::sendRandomOrder,
            1,      // întârziere inițială
            1,      // perioadă
            TimeUnit.SECONDS
        );
        System.out.println("✓ Bot " + clientId + " pornit și conectat la " + serverHost + ":" + serverPort);
    }
    
    /**
     * Oprește botul.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        disconnect();
    }
    
    /**
     * Trimite un ordin aleator către server prin socket.
     * Protocol: SUBMIT|clientId|instrumentId|orderType|volume|limitPrice
     */
    private void sendRandomOrder() {
        if (!running) {
            return;
        }
        
        try {
            // Alege instrument aleator
            Instrument[] instrumentArray = instruments.values().toArray(new Instrument[0]);
            Instrument instrument = instrumentArray[random.nextInt(instrumentArray.length)];
            
            // Alege tip ordin aleator
            OrderType orderType = random.nextBoolean() ? OrderType.BUY_LIMIT : OrderType.SELL_LIMIT;
            
            // Generează volum aleator (între 10 și 100)
            double volume = 10 + random.nextDouble() * 90;
            
            // Strategie: 50% ordine conservatoare (vor expira), 50% agresive (se execută)
            double currentPrice = instrument.getCurrentPrice();
            double limitPrice;
            boolean isConservative = random.nextDouble() < 0.5; // 50% șansă
            
            if (orderType == OrderType.BUY_LIMIT) {
                if (isConservative) {
                    // BUY conservator: vrei să cumperi IEFTIN (sub preț curent)
                    // Limită 20-30% SUB preț - va aștepta ca prețul să scadă
                    limitPrice = currentPrice * (0.70 + random.nextDouble() * 0.10); // 70-80% din preț
                } else {
                    // BUY agresiv: accepți să plătești mai mult
                    // Limită 0-5% PESTE preț - se execută imediat
                    limitPrice = currentPrice * (1.0 + random.nextDouble() * 0.05);
                }
            } else {
                if (isConservative) {
                    // SELL conservator: vrei să vinzi SCUMP (peste preț curent)
                    // Limită 20-30% PESTE preț - va aștepta ca prețul să crească
                    limitPrice = currentPrice * (1.20 + random.nextDouble() * 0.10); // 120-130% din preț
                } else {
                    // SELL agresiv: accepți să primești mai puțin
                    // Limită 0-5% SUB preț - se execută imediat
                    limitPrice = currentPrice * (0.95 + random.nextDouble() * 0.05);
                }
            }
            
            System.out.printf("\n[%s] Trimite ordin: %s %s %.2f @ %.2f (curent: %.2f)\n",
                            clientId, orderType, instrument.getId(), 
                            volume, limitPrice, currentPrice);
            
            // Construiește mesajul pentru server
            // Format: SUBMIT|clientId|instrumentId|orderType|volume|limitPrice
            String message = String.format("SUBMIT|%s|%s|%s|%.2f|%.2f",
                                         clientId, 
                                         instrument.getId(),
                                         orderType.name(),
                                         volume,
                                         limitPrice);
            
            // Trimite mesajul către server
            out.println(message);
            
            // Primește răspunsul (sincron pentru simplitate)
            String response = in.readLine();
            
            if (response != null) {
                // Parse răspuns: ACCEPTED|orderId sau REJECTED|reason
                String[] parts = response.split("\\|", 2);
                String status = parts[0];
                String details = parts.length > 1 ? parts[1] : "";
                
                if ("ACCEPTED".equals(status)) {
                    System.out.printf("[%s] ✓ Ordin %s ACCEPTAT\n", clientId, details);
                } else if ("REJECTED".equals(status)) {
                    System.out.printf("[%s] ✗ Ordin RESPINS: %s\n", clientId, details);
                } else if ("ERROR".equals(status)) {
                    System.err.printf("[%s] Eroare server: %s\n", clientId, details);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Eroare comunicare bot " + clientId + ": " + e.getMessage());
            // Încearcă reconectare
            disconnect();
            if (running) {
                connect();
            }
        } catch (Exception e) {
            System.err.println("Eroare în bot " + clientId + ": " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        start();
    }
}
