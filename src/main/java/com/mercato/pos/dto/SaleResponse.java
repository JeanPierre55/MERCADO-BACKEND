package com.mercato.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para representar una venta en la respuesta.
 */
public record SaleResponse(
    String id,
    String terminalId,
    String cashierId,
    String customerId,
    String status,
    List<SaleItemDto> items,
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal discount,
    BigDecimal total,
    String paymentType,
    LocalDateTime createdAt
) {}
