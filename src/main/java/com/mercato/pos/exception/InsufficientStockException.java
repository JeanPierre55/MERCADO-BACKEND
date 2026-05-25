package com.mercato.pos.exception;

/**
 * Excepción lanzada cuando no hay stock suficiente para un producto.
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
