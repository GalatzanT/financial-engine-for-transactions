package model;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Reprezintă un ordin de tranzacționare plasat de un client.
 */
public class Order {
    private final String orderId;
    private final String clientId;
    private final Instrument instrument;
    private final OrderType orderType;
    private final double volume;
    private final double limitPrice;
    private volatile OrderStatus status;
    private final LocalDateTime timestamp;
    private final CompletableFuture<OrderStatus> resultFuture;
    
    /**
     * Constructor pentru un nou ordin.
     * 
     * @param orderId ID unic al ordinului
     * @param clientId ID-ul clientului
     * @param instrument Instrumentul financiar
     * @param orderType Tipul ordinului (BUY_LIMIT / SELL_LIMIT)
     * @param volume Volumul dorit
     * @param limitPrice Prețul limită
     */
    public Order(String orderId, String clientId, Instrument instrument, 
                OrderType orderType, double volume, double limitPrice) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.instrument = instrument;
        this.orderType = orderType;
        this.volume = volume;
        this.limitPrice = limitPrice;
        this.status = OrderStatus.PENDING;
        this.timestamp = LocalDateTime.now();
        this.resultFuture = new CompletableFuture<>();
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public Instrument getInstrument() {
        return instrument;
    }
    
    public OrderType getOrderType() {
        return orderType;
    }
    
    public double getVolume() {
        return volume;
    }
    
    public double getLimitPrice() {
        return limitPrice;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
        // Notifică clientul cu statusul final
        if (status != OrderStatus.PENDING) {
            resultFuture.complete(status);
        }
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public CompletableFuture<OrderStatus> getResultFuture() {
        return resultFuture;
    }
    
    /**
     * Verifică dacă ordinul a expirat (peste 10 secunde de la plasare).
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(timestamp.plusSeconds(10));
    }
    
    /**
     * Verifică dacă prețul curent satisface condiția de execuție.
     */
    public boolean canExecute(double currentPrice) {
        if (orderType == OrderType.BUY_LIMIT) {
            // Pentru BUY_LIMIT: execută dacă prețul curent <= preț limită
            return currentPrice <= limitPrice;
        } else {
            // Pentru SELL_LIMIT: execută dacă prețul curent >= preț limită
            return currentPrice >= limitPrice;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Order[%s, client=%s, %s, %s, vol=%.2f, limit=%.2f, status=%s]",
                           orderId, clientId, instrument.getId(), orderType, 
                           volume, limitPrice, status);
    }
}
