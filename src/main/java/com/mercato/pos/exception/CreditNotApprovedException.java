package com.mercato.pos.exception;

/**
 * Excepción lanzada cuando el crédito del cliente no está aprobado.
 */
public class CreditNotApprovedException extends RuntimeException {
    public CreditNotApprovedException(String message) {
        super(message);
    }
}
