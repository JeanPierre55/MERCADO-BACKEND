package com.mercato.pos.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Tests unitarios para la entidad ReturnRecord.
 */
class ReturnRecordEntityTest {

    @Test
    void testReturnRecordCreation() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        // Act
        ReturnRecord record = new ReturnRecord("SALE001", "PROD001", 2, "Defective", now);

        // Assert
        assertNotNull(record.getId());
        assertEquals("SALE001", record.getSaleId());
        assertEquals("PROD001", record.getProductId());
        assertEquals(2, record.getQuantityReturned());
        assertEquals("Defective", record.getReturnReason());
        assertEquals(now, record.getReturnedAt());
    }

    @Test
    void testReturnRecordWithReceiptId() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        // Act
        ReturnRecord record = new ReturnRecord("SALE001", "PROD001", 2, "Defective", now);
        record.setReceiptId("RECEIPT001");

        // Assert
        assertEquals("RECEIPT001", record.getReceiptId());
    }

    @Test
    void testMultipleReturnRecords() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        // Act
        ReturnRecord record1 = new ReturnRecord("SALE001", "PROD001", 2, "Defective", now);
        ReturnRecord record2 = new ReturnRecord("SALE001", "PROD002", 1, "Wrong size", now);

        // Assert
        assertEquals("PROD001", record1.getProductId());
        assertEquals("PROD002", record2.getProductId());
        assertNotEquals(record1.getId(), record2.getId());
    }
}
