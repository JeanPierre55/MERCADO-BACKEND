package com.mercato.pos.dto;

import java.math.BigDecimal;

public record ProductDto(
    String productId,
    String name,
    String barcode,
    BigDecimal unitPrice,
    int availableStock,
    String category
) {}
