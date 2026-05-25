package com.mercato.pos.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.mercato.pos.model.Receipt;

/**
 * Tests de integración para ReceiptRepository.
 */
@DataJpaTest
class ReceiptRepositoryTest {

    @Autowired
    private ReceiptRepository receiptRepository;

    private Receipt receipt1;
    private Receipt receipt2;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        receipt1 = new Receipt("TXN001", "SALE001", "SALE", "{}", now);
        receipt2 = new Receipt("TXN002", "SALE002", "RETURN", "{}", now);

        receiptRepository.save(receipt1);
        receiptRepository.save(receipt2);
    }

    @Test
    void testFindByTransactionId() {
        // Act
        Optional<Receipt> result = receiptRepository.findByTransactionId("TXN001");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("TXN001", result.get().getTransactionId());
        assertEquals("SALE001", result.get().getSaleId());
    }

    @Test
    void testFindByTransactionId_NotFound() {
        // Act
        Optional<Receipt> result = receiptRepository.findByTransactionId("NONEXISTENT");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testSaveReceipt() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Receipt newReceipt = new Receipt("TXN003", "SALE003", "SALE", "{}", now);

        // Act
        Receipt saved = receiptRepository.save(newReceipt);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("TXN003", saved.getTransactionId());
    }

    @Test
    void testReceiptUniquenessConstraint() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Receipt duplicate = new Receipt("TXN001", "SALE003", "SALE", "{}", now);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            receiptRepository.save(duplicate);
            receiptRepository.flush();
        });
    }
}
