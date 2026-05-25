package com.mercato.pos.dto;

import java.math.BigDecimal;

/**
 * DTO para representar un item de venta en la respuesta.
 */
public record SaleItemDto(
    String id,
    String productId,
    String productName,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal lineTotal
) {}
