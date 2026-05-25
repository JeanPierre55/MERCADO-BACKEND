package com.mercato.pos.exception;

/**
 * Excepción lanzada cuando se intenta devolver una venta que ya fue devuelta.
 */
public class SaleAlreadyReturnedException extends RuntimeException {
    public SaleAlreadyReturnedException(String message) {
        super(message);
    }
}
