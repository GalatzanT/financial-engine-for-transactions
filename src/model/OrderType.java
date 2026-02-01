package model;

/**
 * Tipurile de ordine suportate de sistemul de tranzacționare.
 */
public enum OrderType {
    /**
     * Ordin de cumpărare cu preț limită maximă
     */
    BUY_LIMIT,
    
    /**
     * Ordin de vânzare cu preț limită minimă
     */
    SELL_LIMIT
}
