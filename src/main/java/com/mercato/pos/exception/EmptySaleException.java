package com.mercato.pos.exception;

/**
 * Excepción lanzada cuando se intenta procesar una venta sin items.
 */
public class EmptySaleException extends RuntimeException {
    public EmptySaleException(String message) {
        super(message);
    }
}
