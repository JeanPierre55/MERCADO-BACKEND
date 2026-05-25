package com.mercato.pos.exception;

/**
 * Excepción lanzada cuando no se encuentra una venta.
 */
public class SaleNotFoundException extends RuntimeException {
    public SaleNotFoundException(String message) {
        super(message);
    }
}
