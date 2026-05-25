package com.mercato.pos.exception;

/**
 * Excepción lanzada cuando se intenta modificar una venta que no está en estado ACTIVE.
 */
public class SaleNotActiveException extends RuntimeException {
    public SaleNotActiveException(String message) {
        super(message);
    }
}
