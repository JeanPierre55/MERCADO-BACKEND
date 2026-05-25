package com.mercato.pos.exception;

/**
 * Excepción lanzada cuando se intenta operar sobre una venta que no está congelada.
 */
public class SaleNotFrozenException extends RuntimeException {
    public SaleNotFrozenException(String message) {
        super(message);
    }
}
