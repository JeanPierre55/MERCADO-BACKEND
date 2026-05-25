package com.mercato.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * DTO para solicitud de devolución parcial.
 */
public record PartialReturnRequest(
    @NotEmpty(message = "La lista de items a devolver no debe estar vacía")
    @Valid
    List<ReturnItemRequest> items
) {}
