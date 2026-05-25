package com.mercato.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para solicitud de checkout.
 */
public record CheckoutRequest(
    @NotBlank(message = "El tipo de pago no debe estar vacío")
    String paymentType,
    
    @NotNull(message = "El monto recibido no debe ser nulo")
    BigDecimal amountReceived
) {}
