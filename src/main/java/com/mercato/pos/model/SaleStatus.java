package com.mercato.pos.model;

/**
 * Estados posibles de una venta en el ciclo de vida.
 */
public enum SaleStatus {
    /**
     * Venta recién creada, aceptando items.
     */
    ACTIVE,

    /**
     * Venta completada exitosamente (checkout realizado).
     */
    COMPLETED,

    /**
     * Venta cancelada antes del checkout.
     */
    CANCELLED,

    /**
     * Venta pausada temporalmente por el cajero.
     */
    FROZEN,

    /**
     * Venta completamente devuelta.
     */
    RETURNED,

    /**
     * Venta con devolución parcial de items.
     */
    PARTIALLY_RETURNED
}
