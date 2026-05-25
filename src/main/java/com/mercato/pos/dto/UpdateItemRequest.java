package com.mercato.pos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para solicitud de actualizar cantidad de item en una venta.
 */
public record UpdateItemRequest(
    @NotNull(message = "La cantidad no puede ser nula")
    @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1")
    Integer quantity
) {}
