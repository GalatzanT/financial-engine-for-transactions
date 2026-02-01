package server;

import model.*;
import util.FileLogger;
import java.util.concurrent.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Motorul principal de tranzacționare.
 * Gestionează primirea ordinelor, validarea lichidității și orchestrarea execuției.
 */
public class TradingEngine {
    private final Map<String, Instrument> instruments;
    private final LiquidityManager liquidityManager;
    private final BlockingQueue<Order> pendingOrders;
    private final ExecutorService workerPool;
    private final AuditService auditService;
    
    // Liste thread-safe pentru tracking
    private final List<Order> allOrders;
    
    // Profit per instrument (thread-safe)
    private final Map<String, Double> profitPerInstrument; // Comisioane
    private final Map<String, Double> pnlPerInstrument; // Profit/Pierdere din tranzacții
    
    private volatile boolean running;
    
    /**
     * Constructor pentru Trading Engine.
     * 
     * @param instruments Map cu instrumentele disponibile
     * @param numThreads Numărul de thread-uri în pool
     */
    public TradingEngine(Map<String, Instrument> instruments, int numThreads) {
        this.instruments = instruments;
        this.liquidityManager = new LiquidityManager();
        this.pendingOrders = new LinkedBlockingQueue<>();
        this.workerPool = Executors.newFixedThreadPool(numThreads);
        this.allOrders = new CopyOnWriteArrayList<>();
        this.profitPerInstrument = new ConcurrentHashMap<>();
        this.pnlPerInstrument = new ConcurrentHashMap<>();
        this.running = false;
        
        // Inițializează lichiditatea și profitul pentru toate instrumentele
        for (Instrument instrument : instruments.values()) {
            liquidityManager.initializeLiquidity(instrument);
            profitPerInstrument.put(instrument.getId(), 0.0);
            pnlPerInstrument.put(instrument.getId(), 0.0);
        }
        
        // Inițializează serviciul de audit
        this.auditService = new AuditService(this, liquidityManager, instruments);
        
        // Inițializează fișierele
        initializeLogFiles();
    }
    
    /**
     * Inițializează fișierele de logging.
     */
    private void initializeLogFiles() {
        FileLogger.initializeFile("orders.txt", "LOG ORDINE - Financial Engine");
        FileLogger.initializeFile("executions.txt", "LOG EXECUȚII - Financial Engine");
        FileLogger.initializeFile("cancellations.txt", "LOG ANULĂRI - Financial Engine");
        FileLogger.initializeFile("audit_log.txt", "LOG AUDIT - Financial Engine");
    }
    
    /**
     * Pornește motorul de tranzacționare.
     */
    public void start() {
        running = true;
        auditService.start();
        System.out.println("Trading Engine pornit!");
    }
    
    /**
     * Oprește motorul de tranzacționare.
     */
    public void shutdown() {
        System.out.println("\n=== Oprire Trading Engine ===");
        running = false;
        
        // Oprește serviciul de audit
        auditService.stop();
        
        // Oprește worker pool-ul
        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Trading Engine oprit!");
    }
    
    /**
     * Primește un ordin de la client.
     * 
     * @param order Ordinul primit
     * @return Future cu rezultatul
     */
    public CompletableFuture<OrderStatus> submitOrder(Order order) {
        // Validare lichiditate
        // Atât BUY cât și SELL consumă lichiditate (capacitate de procesare)
        boolean reserved = liquidityManager.reserveVolume(
            order.getInstrument().getId(), 
            order.getVolume()
        );
        
        if (!reserved) {
            // Lichiditate insuficientă - respinge ordinul
            order.setStatus(OrderStatus.REJECTED);
            FileLogger.logWithTimestamp("orders.txt", 
                order + " - REJECTED (lichiditate insuficientă)");
            System.out.println("❌ " + order.getOrderId() + " RESPINS (lichiditate insuficientă)");
            return order.getResultFuture();
        }
        
        // Ordin acceptat - adaugă în coadă
        allOrders.add(order);
        pendingOrders.offer(order);
        FileLogger.logWithTimestamp("orders.txt", order + " - ACCEPTAT");
        System.out.println("✓ " + order.getOrderId() + " ACCEPTAT în coadă");
        
        return order.getResultFuture();
    }
    
    /**
     * Obține toate ordinele pending pentru procesare.
     */
    public List<Order> getPendingOrders() {
        return new ArrayList<>(pendingOrders);
    }
    
    /**
     * Șterge un ordin din coada de pending (după procesare).
     */
    public void removeFromPending(Order order) {
        pendingOrders.remove(order);
    }
    
    /**
     * Execută un ordin și calculează comisionul și P&L.
     */
    public void executeOrder(Order order) {
        double executionPrice = order.getInstrument().getCurrentPrice();
        order.setStatus(OrderStatus.EXECUTED);
        
        Execution execution = new Execution(order, executionPrice);
        
        // 1. Comision (0.5% din valoarea tranzacției)
        double commission = execution.getCommission();
        String instrumentId = order.getInstrument().getId();
        profitPerInstrument.merge(instrumentId, commission, Double::sum);
        
        // 2. P&L din tranzacție (market maker perspective)
        double transactionValue = executionPrice * order.getVolume();
        double pnl;
        if (order.getOrderType() == OrderType.BUY_LIMIT) {
            // Client cumpără → Server vinde → Incăsăm bani (+)
            pnl = transactionValue;
        } else {
            // Client vinde → Server cumpără → Plătim bani (-)
            pnl = -transactionValue;
        }
        pnlPerInstrument.merge(instrumentId, pnl, Double::sum);
        
        FileLogger.logWithTimestamp("executions.txt", execution.toString());
        
        System.out.println("✅ EXECUTAT: " + order.getOrderId() + 
                         " la prețul " + String.format("%.2f", executionPrice) +
                         " | Comision: " + String.format("%.2f", commission));
    }
    
    /**
     * Anulează un ordin (expirat).
     */
    public void cancelOrder(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        
        // Eliberează lichiditatea (ambele tipuri au rezervat la submit)
        liquidityManager.releaseVolume(
            order.getInstrument().getId(), 
            order.getVolume()
        );
        
        FileLogger.logWithTimestamp("cancellations.txt", 
            order.getOrderId() + " | " + order.getInstrument().getId() + 
            " | Expirat după 10 secunde");
        
        System.out.println("⏱️ ANULAT (expirat): " + order.getOrderId());
    }
    
    public Map<String, Instrument> getInstruments() {
        return instruments;
    }
    
    public LiquidityManager getLiquidityManager() {
        return liquidityManager;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Obține profitul total per instrument (din comisioane).
     */
    public Map<String, Double> getProfitPerInstrument() {
        return new HashMap<>(profitPerInstrument);
    }
    
    /**
     * Obține P&L per instrument (incăsări - plăți).
     */
    public Map<String, Double> getPnLPerInstrument() {
        return new HashMap<>(pnlPerInstrument);
    }
}
