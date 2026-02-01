package server;

import model.*;
import util.FileLogger;
import util.PriceSimulator;
import java.util.concurrent.*;
import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Serviciu de audit care rulează periodic (la fiecare 2 secunde).
 * Responsabil pentru:
 * - Actualizarea prețurilor
 * - Procesarea ordinelor pending (verificare expirare și execuție)
 * - Verificarea integrității
 * - Logging audit
 */
public class AuditService {
    private static final DateTimeFormatter TIME_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final TradingEngine engine;
    private final LiquidityManager liquidityManager;
    private final Map<String, Instrument> instruments;
    private final PriceSimulator priceSimulator;
    private final ScheduledExecutorService scheduler;
    
    /**
     * Constructor pentru serviciul de audit.
     */
    public AuditService(TradingEngine engine, LiquidityManager liquidityManager,
                       Map<String, Instrument> instruments) {
        this.engine = engine;
        this.liquidityManager = liquidityManager;
        this.instruments = instruments;
        this.priceSimulator = new PriceSimulator(2.0); // dt = 2 secunde
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    /**
     * Pornește serviciul de audit (rulează la fiecare 2 secunde).
     */
    public void start() {
        scheduler.scheduleAtFixedRate(
            this::runAudit, 
            2,      // întârziere inițială
            2,      // perioadă
            TimeUnit.SECONDS
        );
        System.out.println("Audit Service pornit (interval: 2 secunde)");
    }
    
    /**
     * Oprește serviciul de audit.
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Audit Service oprit");
    }
    
    /**
     * Execută ciclul de audit.
     */
    private void runAudit() {
        try {
            System.out.println("\n--- AUDIT CYCLE: " + 
                             LocalDateTime.now().format(TIME_FORMAT) + " ---");
            
            // 1. Actualizează prețurile
            updatePrices();
            
            // 2. Procesează ordinele pending
            processOrders();
            
            // 3. Verifică integritatea
            checkIntegrity();
            
            // 4. Calculează profitul
            calculateProfit();
            
            // 5. Scrie log de audit
            writeAuditLog();
            
        } catch (Exception e) {
            System.err.println("Eroare în audit cycle: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Actualizează prețurile tuturor instrumentelor.
     */
    private void updatePrices() {
        priceSimulator.updateAllPrices(instruments.values());
        
        System.out.println("Prețuri actualizate:");
        for (Instrument instrument : instruments.values()) {
            System.out.printf("  %s: %.2f\n", 
                            instrument.getId(), 
                            instrument.getCurrentPrice());
        }
    }
    
    /**
     * Procesează ordinele pending (verifică expirare și condiții de execuție).
     */
    private void processOrders() {
        List<Order> pending = engine.getPendingOrders();
        int executed = 0;
        int cancelled = 0;
        
        for (Order order : pending) {
            if (order.getStatus() != OrderStatus.PENDING) {
                continue;
            }
            
            // Verifică expirare
            if (order.isExpired()) {
                engine.cancelOrder(order);
                engine.removeFromPending(order);
                cancelled++;
                continue;
            }
            
            // Verifică condiție de execuție
            double currentPrice = order.getInstrument().getCurrentPrice();
            if (order.canExecute(currentPrice)) {
                engine.executeOrder(order);
                engine.removeFromPending(order);
                executed++;
            }
        }
        
        System.out.printf("Procesare: %d executate, %d anulate, %d rămase\n",
                        executed, cancelled, engine.getPendingOrders().size());
    }
    
    /**
     * Verifică integritatea volumelor (nicio depășire V_max).
     */
    private void checkIntegrity() {
        boolean integrity = liquidityManager.checkIntegrity(instruments);
        if (integrity) {
            System.out.println("✓ Integritate verificată - OK");
        } else {
            System.err.println("✗ EROARE DE INTEGRITATE!");
        }
    }
    
    /**
     * Calculează și afișează profitul per instrument.
     */
    private void calculateProfit() {
        System.out.println("Comisioane per Instrument:");
        Map<String, Double> commissions = engine.getProfitPerInstrument();
        double totalCommission = 0.0;
        
        for (Map.Entry<String, Double> entry : commissions.entrySet()) {
            double commission = entry.getValue();
            totalCommission += commission;
            System.out.printf("  %s: %.2f\n", entry.getKey(), commission);
        }
        System.out.printf("Total Comisioane: %.2f\n", totalCommission);
        
        System.out.println("\nP&L per Instrument (Încasări - Plăți):");
        Map<String, Double> pnl = engine.getPnLPerInstrument();
        double totalPnL = 0.0;
        
        for (Map.Entry<String, Double> entry : pnl.entrySet()) {
            double value = entry.getValue();
            totalPnL += value;
            String status = value >= 0 ? "Profit" : "Pierdere";
            System.out.printf("  %s: %.2f (%s)\n", entry.getKey(), value, status);
        }
        System.out.printf("Total P&L: %.2f\n", totalPnL);
        
        double netProfit = totalCommission + totalPnL;
        System.out.printf("\n✅ PROFIT NET: %.2f (Comisioane: %.2f + P&L: %.2f)\n", 
                         netProfit, totalCommission, totalPnL);
    }
    
    /**
     * Scrie log-ul de audit în fișier.
     */
    private void writeAuditLog() {
        StringBuilder log = new StringBuilder();
        log.append("\n=== AUDIT ").append(LocalDateTime.now().format(TIME_FORMAT)).append(" ===\n");
        
        // Prețuri curente
        log.append("PREȚURI CURENTE:\n");
        for (Instrument instrument : instruments.values()) {
            log.append(String.format("  %s: %.2f\n", 
                                   instrument.getId(), 
                                   instrument.getCurrentPrice()));
        }
        
        // Lichiditate disponibilă
        log.append("\nLICHIDITATE DISPONIBILĂ:\n");
        for (Instrument instrument : instruments.values()) {
            double available = liquidityManager.getAvailableLiquidity(instrument.getId());
            log.append(String.format("  %s: %.2f / %.2f\n",
                                   instrument.getId(),
                                   available,
                                   instrument.getMaxLiquidity()));
        }
        
        // Comisioane per instrument
        log.append("\nCOMISIOANE PER INSTRUMENT:\n");
        Map<String, Double> commissions = engine.getProfitPerInstrument();
        double totalCommission = 0.0;
        for (Map.Entry<String, Double> entry : commissions.entrySet()) {
            double commission = entry.getValue();
            totalCommission += commission;
            log.append(String.format("  %s: %.2f\n", entry.getKey(), commission));
        }
        log.append(String.format("TOTAL COMISIOANE: %.2f\n", totalCommission));
        
        // P&L per instrument
        log.append("\nP&L PER INSTRUMENT (Incăsări - Plăți):\n");
        Map<String, Double> pnl = engine.getPnLPerInstrument();
        double totalPnL = 0.0;
        for (Map.Entry<String, Double> entry : pnl.entrySet()) {
            double value = entry.getValue();
            totalPnL += value;
            String status = value >= 0 ? "Profit" : "Pierdere";
            log.append(String.format("  %s: %.2f (%s)\n", entry.getKey(), value, status));
        }
        log.append(String.format("TOTAL P&L: %.2f\n", totalPnL));
        
        double netProfit = totalCommission + totalPnL;
        log.append(String.format("PROFIT NET: %.2f\n", netProfit));
        
        // Ordine pending
        List<Order> pending = engine.getPendingOrders();
        log.append("\nORDINE PENDING: ").append(pending.size()).append("\n");
        for (Order order : pending) {
            if (order.getStatus() == OrderStatus.PENDING) {
                log.append("  ").append(order).append("\n");
            }
        }
        
        FileLogger.log("audit_log.txt", log.toString());
    }
}
