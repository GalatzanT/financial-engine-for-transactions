package model;

/**
 * Reprezintă un instrument financiar tranzacționabil.
 */
public class Instrument {
    private final String id;
    private volatile double currentPrice;
    private final double maxLiquidity;
    private final double volatility;  // sigma
    private final double trend;       // mu
    
    /**
     * Constructor pentru un instrument financiar.
     * 
     * @param id Identificatorul unic al instrumentului
     * @param initialPrice Prețul inițial
     * @param maxLiquidity Lichiditatea maximă disponibilă (V_max)
     * @param volatility Volatilitatea (sigma)
     * @param trend Trendul (mu)
     */
    public Instrument(String id, double initialPrice, double maxLiquidity, 
                     double volatility, double trend) {
        this.id = id;
        this.currentPrice = initialPrice;
        this.maxLiquidity = maxLiquidity;
        this.volatility = volatility;
        this.trend = trend;
    }
    
    public String getId() {
        return id;
    }
    
    public synchronized double getCurrentPrice() {
        return currentPrice;
    }
    
    public synchronized void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public double getMaxLiquidity() {
        return maxLiquidity;
    }
    
    public double getVolatility() {
        return volatility;
    }
    
    public double getTrend() {
        return trend;
    }
    
    @Override
    public String toString() {
        return String.format("Instrument[%s, price=%.2f, maxLiq=%.2f]", 
                           id, currentPrice, maxLiquidity);
    }
}
