package com.mercato.pos.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para solicitud de creación de venta.
 */
public record CreateSaleRequest(
    @NotBlank(message = "El ID del terminal no puede estar vacío")
    String terminalId,
    
    String customerId
) {}
