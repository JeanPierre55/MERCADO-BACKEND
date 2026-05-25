package com.mercato.pos.exception;

/**
 * Excepción lanzada cuando la cantidad de un item es inválida.
 */
public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException(String message) {
        super(message);
    }
}
