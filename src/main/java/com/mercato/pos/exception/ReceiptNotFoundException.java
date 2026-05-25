package com.mercato.pos.exception;

/**
 * Excepción lanzada cuando no se encuentra un recibo.
 */
public class ReceiptNotFoundException extends RuntimeException {
    public ReceiptNotFoundException(String message) {
        super(message);
    }
}
