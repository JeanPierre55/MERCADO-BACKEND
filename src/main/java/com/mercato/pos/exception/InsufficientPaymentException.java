package com.mercato.pos.exception;

/**
 * Excepción lanzada cuando el monto de pago es insuficiente.
 */
public class InsufficientPaymentException extends RuntimeException {
    public InsufficientPaymentException(String message) {
        super(message);
    }
}
