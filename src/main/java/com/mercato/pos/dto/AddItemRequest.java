package com.mercato.pos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para solicitud de agregar item a una venta.
 */
public record AddItemRequest(
    String productId,
    
    String barcode,
    
    @NotNull(message = "La cantidad no puede ser nula")
    @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1")
    Integer quantity
) {}
