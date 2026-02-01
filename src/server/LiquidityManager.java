package server;

import model.Instrument;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Gestionează lichiditatea disponibilă pentru fiecare instrument financiar.
 * Thread-safe prin utilizarea ConcurrentHashMap și metode sincronizate.
 */
public class LiquidityManager {
    // Map: instrumentId -> lichiditate disponibilă curentă
    private final Map<String, Double> availableLiquidity;
    
    /**
     * Constructor care inițializează lichiditatea pentru instrumentele date.
     */
    public LiquidityManager() {
        this.availableLiquidity = new ConcurrentHashMap<>();
    }
    
    /**
     * Inițializează lichiditatea pentru un instrument.
     */
    public void initializeLiquidity(Instrument instrument) {
        availableLiquidity.put(instrument.getId(), instrument.getMaxLiquidity());
    }
    
    /**
     * Verifică și rezervă volumul pentru un ordin.
     * 
     * @param instrumentId ID-ul instrumentului
     * @param volume Volumul cerut
     * @return true dacă există lichiditate și s-a rezervat cu succes
     */
    public synchronized boolean reserveVolume(String instrumentId, double volume) {
        Double available = availableLiquidity.get(instrumentId);
        if (available == null) {
            return false;
        }
        
        if (available >= volume) {
            availableLiquidity.put(instrumentId, available - volume);
            return true;
        }
        
        return false;
    }
    
    /**
     * Eliberează volumul rezervat (de exemplu, când ordinul expiră).
     */
    public synchronized void releaseVolume(String instrumentId, double volume) {
        Double available = availableLiquidity.get(instrumentId);
        if (available != null) {
            availableLiquidity.put(instrumentId, available + volume);
        }
    }
    
    /**
     * Obține lichiditatea disponibilă pentru un instrument.
     */
    public double getAvailableLiquidity(String instrumentId) {
        return availableLiquidity.getOrDefault(instrumentId, 0.0);
    }
    
    /**
     * Verifică integritatea: nicio lichiditate nu depășește maximul.
     */
    public synchronized boolean checkIntegrity(Map<String, Instrument> instruments) {
        for (Map.Entry<String, Instrument> entry : instruments.entrySet()) {
            String id = entry.getKey();
            Instrument instrument = entry.getValue();
            double available = availableLiquidity.getOrDefault(id, 0.0);
            
            if (available > instrument.getMaxLiquidity()) {
                System.err.println("EROARE INTEGRITATE: Instrument " + id + 
                                 " are lichiditate " + available + 
                                 " > max " + instrument.getMaxLiquidity());
                return false;
            }
        }
        return true;
    }
    
    /**
     * Afișează statusul lichidității pentru toate instrumentele.
     */
    public String getLiquidityStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Status Lichiditate:\n");
        for (Map.Entry<String, Double> entry : availableLiquidity.entrySet()) {
            sb.append(String.format("  %s: %.2f disponibil\n", 
                                  entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }
}
