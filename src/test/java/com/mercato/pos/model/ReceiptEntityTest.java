package com.mercato.pos.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Tests unitarios para la entidad Receipt.
 */
class ReceiptEntityTest {

    @Test
    void testReceiptCreation() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        String receiptJson = "{\"transactionId\": \"TXN001\"}";

        // Act
        Receipt receipt = new Receipt("TXN001", "SALE001", "SALE", receiptJson, now);

        // Assert
        assertNotNull(receipt.getId());
        assertEquals("TXN001", receipt.getTransactionId());
        assertEquals("SALE001", receipt.getSaleId());
        assertEquals("SALE", receipt.getReceiptType());
        assertEquals(receiptJson, receipt.getReceiptJson());
        assertEquals(now, receipt.getGeneratedAt());
    }

    @Test
    void testReceiptWithOriginalTransactionId() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        String receiptJson = "{\"originalTransactionId\": \"TXN001\"}";

        // Act
        Receipt receipt = new Receipt("TXN002", "SALE001", "RETURN", receiptJson, now);
        receipt.setOriginalTransactionId("TXN001");

        // Assert
        assertEquals("TXN002", receipt.getTransactionId());
        assertEquals("TXN001", receipt.getOriginalTransactionId());
        assertEquals("RETURN", receipt.getReceiptType());
    }

    @Test
    void testReceiptTypes() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        // Act & Assert
        Receipt saleReceipt = new Receipt("TXN001", "SALE001", "SALE", "{}", now);
        assertEquals("SALE", saleReceipt.getReceiptType());

        Receipt returnReceipt = new Receipt("TXN002", "SALE001", "RETURN", "{}", now);
        assertEquals("RETURN", returnReceipt.getReceiptType());

        Receipt partialReturnReceipt = new Receipt("TXN003", "SALE001", "PARTIAL_RETURN", "{}", now);
        assertEquals("PARTIAL_RETURN", partialReturnReceipt.getReceiptType());
    }
}
