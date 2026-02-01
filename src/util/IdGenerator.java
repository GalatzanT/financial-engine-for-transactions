package util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generator simplu de ID-uri unice pentru ordine.
 */
public class IdGenerator {
    private static final AtomicLong orderCounter = new AtomicLong(1);
    
    /**
     * Generează un ID unic pentru un nou ordin.
     */
    public static String generateOrderId() {
        return "ORD-" + orderCounter.getAndIncrement();
    }
}
