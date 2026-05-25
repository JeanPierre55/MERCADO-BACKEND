package com.mercato.pos.dto;

public record CustomerDto(
    String customerId,
    String fullName,
    String documentType,
    String documentNumber,
    String creditStatus
) {}
