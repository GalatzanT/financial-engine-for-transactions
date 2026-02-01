package client;

import model.*;
import server.TradingEngine;
import util.IdGenerator;
import java.util.Random;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Simulator de client (bot de tranzacționare).
 * Trimite ordine aleatorii către Trading Engine.
 */
public class TradingBot implements Runnable {
    private final String clientId;
    private final TradingEngine engine;
    private final Map<String, Instrument> instruments;
    private final Random random;
    private final ScheduledExecutorService scheduler;
    
    /**
     * Constructor pentru bot.
     * 
     * @param clientId ID-ul clientului
     * @param engine Referință către Trading Engine
     * @param instruments Map cu instrumentele disponibile
     */
    public TradingBot(String clientId, TradingEngine engine, 
                     Map<String, Instrument> instruments) {
        this.clientId = clientId;
        this.engine = engine;
        this.instruments = instruments;
        this.random = new Random();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    /**
     * Pornește botul (trimite ordine la fiecare 1 secundă).
     */
    public void start() {
        scheduler.scheduleAtFixedRate(
            this::sendRandomOrder,
            1,      // întârziere inițială
            1,      // perioadă
            TimeUnit.SECONDS
        );
        System.out.println("Bot " + clientId + " pornit");
    }
    
    /**
     * Oprește botul.
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Trimite un ordin aleator.
     */
    private void sendRandomOrder() {
        if (!engine.isRunning()) {
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
            
            // Generează preț limită (în jurul prețului curent ± 10%)
            double currentPrice = instrument.getCurrentPrice();
            double limitPrice;
            if (orderType == OrderType.BUY_LIMIT) {
                // Pentru BUY_LIMIT: preț limită puțin peste prețul curent
                limitPrice = currentPrice * (1.0 + random.nextDouble() * 0.1);
            } else {
                // Pentru SELL_LIMIT: preț limită puțin sub prețul curent
                limitPrice = currentPrice * (0.9 + random.nextDouble() * 0.1);
            }
            
            // Creează ordinul
            String orderId = IdGenerator.generateOrderId();
            Order order = new Order(orderId, clientId, instrument, orderType, volume, limitPrice);
            
            System.out.printf("\n[%s] Trimite ordin: %s %s %.2f @ %.2f (curent: %.2f)\n",
                            clientId, orderType, instrument.getId(), 
                            volume, limitPrice, currentPrice);
            
            // Trimite ordinul către engine
            CompletableFuture<OrderStatus> future = engine.submitOrder(order);
            
            // Așteaptă rezultatul (async)
            future.thenAccept(status -> {
                System.out.printf("[%s] Rezultat pentru %s: %s\n", 
                                clientId, orderId, status);
            });
            
        } catch (Exception e) {
            System.err.println("Eroare în bot " + clientId + ": " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        start();
    }
}
