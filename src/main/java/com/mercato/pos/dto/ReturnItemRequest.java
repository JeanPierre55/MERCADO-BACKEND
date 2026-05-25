package com.mercato.pos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para item de devolución parcial.
 */
public record ReturnItemRequest(
    @NotBlank(message = "El ID del producto no debe estar vacío")
    String productId,
    
    @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1")
    int quantity,
    
    @NotBlank(message = "La razón de devolución no debe estar vacía")
    String returnReason
) {}
