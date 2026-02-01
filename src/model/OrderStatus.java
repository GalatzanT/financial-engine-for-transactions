package model;

/**
 * Statusurile posibile ale unui ordin.
 */
public enum OrderStatus {
    /**
     * Ordinul așteaptă execuția
     */
    PENDING,
    
    /**
     * Ordinul a fost executat cu succes
     */
    EXECUTED,
    
    /**
     * Ordinul a fost anulat (expirat)
     */
    CANCELLED,
    
    /**
     * Ordinul a fost respins (lichiditate insuficientă)
     */
    REJECTED
}
