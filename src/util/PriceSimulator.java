package util;

import model.Instrument;
import java.util.Random;

/**
 * Simulator pentru actualizarea prețurilor instrumentelor financiare.
 * Folosește modelul: newPrice = prevPrice + mu * dt + sigma * sqrt(dt) * epsilon
 */
public class PriceSimulator {
    private final Random random;
    private final double dt; // Intervalul de timp (în secunde)
    
    /**
     * Constructor pentru simulator.
     * 
     * @param dt Intervalul de timp pentru actualizare (ex: 2.0 pentru 2 secunde)
     */
    public PriceSimulator(double dt) {
        this.random = new Random();
        this.dt = dt;
    }
    
    /**
     * Actualizează prețul unui instrument conform formulei.
     * newPrice = prevPrice + mu * dt + sigma * sqrt(dt) * epsilon
     */
    public void updatePrice(Instrument instrument) {
        double prevPrice = instrument.getCurrentPrice();
        double mu = instrument.getTrend();
        double sigma = instrument.getVolatility();
        
        // epsilon ~ N(0,1)
        double epsilon = random.nextGaussian();
        
        // Calculează noul preț
        double priceChange = mu * dt + sigma * Math.sqrt(dt) * epsilon;
        double newPrice = prevPrice + priceChange;
        
        // Asigură că prețul rămâne pozitiv
        if (newPrice < 1.0) {
            newPrice = 1.0;
        }
        
        instrument.setCurrentPrice(newPrice);
    }
    
    /**
     * Actualizează prețurile tuturor instrumentelor.
     */
    public void updateAllPrices(Iterable<Instrument> instruments) {
        for (Instrument instrument : instruments) {
            updatePrice(instrument);
        }
    }
}
