package com.mercato.pos.exception;

/**
 * Excepción lanzada cuando se intenta operar sobre una venta que no está completada.
 */
public class SaleNotCompletedException extends RuntimeException {
    public SaleNotCompletedException(String message) {
        super(message);
    }
}
