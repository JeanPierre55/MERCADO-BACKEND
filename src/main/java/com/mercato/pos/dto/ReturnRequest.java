package com.mercato.pos.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para solicitud de devolución total.
 */
public record ReturnRequest(
    @NotBlank(message = "La razón de devolución no debe estar vacía")
    String returnReason
) {}
