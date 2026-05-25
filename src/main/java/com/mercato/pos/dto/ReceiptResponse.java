package com.mercato.pos.dto;

/**
 * DTO para respuesta de recibo.
 */
public record ReceiptResponse(
    String transactionId,
    String creditReferenceNumber,
    String receiptType,
    Object receiptData
) {}
