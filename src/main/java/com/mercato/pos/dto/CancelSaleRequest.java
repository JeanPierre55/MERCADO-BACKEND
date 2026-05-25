package com.mercato.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para solicitud de cancelación de venta.
 */
public record CancelSaleRequest(
    @NotBlank(message = "La razón de cancelación no puede estar vacía")
    @Size(max = 255, message = "La razón de cancelación no puede exceder 255 caracteres")
    String cancellationReason
) {}
