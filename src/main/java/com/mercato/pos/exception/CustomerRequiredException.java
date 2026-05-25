package com.mercato.pos.exception;

/**
 * Excepción lanzada cuando se requiere un cliente pero no está asociado.
 */
public class CustomerRequiredException extends RuntimeException {
    public CustomerRequiredException(String message) {
        super(message);
    }
}
