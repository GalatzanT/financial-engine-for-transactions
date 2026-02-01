package model;

import java.time.LocalDateTime;

/**
 * Reprezintă execuția unui ordin (pentru logging).
 */
public class Execution {
    private final String orderId;
    private final String instrumentId;
    private final OrderType orderType;
    private final double volume;
    private final double executionPrice;
    private final double commission;
    private final LocalDateTime executionTime;
    
    /**
     * Constructor pentru o nouă execuție.
     */
    public Execution(Order order, double executionPrice) {
        this.orderId = order.getOrderId();
        this.instrumentId = order.getInstrument().getId();
        this.orderType = order.getOrderType();
        this.volume = order.getVolume();
        this.executionPrice = executionPrice;
        // Comision = 0.5% din valoarea tranzacției
        this.commission = executionPrice * volume * 0.005;
        this.executionTime = LocalDateTime.now();
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public String getInstrumentId() {
        return instrumentId;
    }
    
    public OrderType getOrderType() {
        return orderType;
    }
    
    public double getVolume() {
        return volume;
    }
    
    public double getExecutionPrice() {
        return executionPrice;
    }
    
    public LocalDateTime getExecutionTime() {
        return executionTime;
    }
    
    public double getCommission() {
        return commission;
    }
    
    @Override
    public String toString() {
        return String.format("%s | Order: %s | %s | %s | Vol: %.2f | Price: %.2f | Comision: %.2f",
                           executionTime, orderId, instrumentId, orderType, 
                           volume, executionPrice, commission);
    }
}
