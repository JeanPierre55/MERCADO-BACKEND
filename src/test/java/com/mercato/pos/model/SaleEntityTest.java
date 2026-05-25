package com.mercato.pos.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Tests unitarios para la entidad Sale.
 */
class SaleEntityTest {

    @Test
    void testSaleCreation() {
        // Arrange & Act
        Sale sale = new Sale("TERM001", "CASH001");

        // Assert
        assertNotNull(sale.getId());
        assertEquals("TERM001", sale.getTerminalId());
        assertEquals("CASH001", sale.getCashierId());
        assertEquals(SaleStatus.ACTIVE, sale.getStatus());
        assertEquals(0L, sale.getSubtotalCents());
        assertEquals(0L, sale.getTaxCents());
        assertEquals(0L, sale.getDiscountCents());
        assertEquals(0L, sale.getTotalCents());
        assertEquals(new BigDecimal("0.19"), sale.getTaxRate());
        assertNotNull(sale.getCreatedAt());
        assertNotNull(sale.getUpdatedAt());
        assertNotNull(sale.getItems());
        assertTrue(sale.getItems().isEmpty());
    }

    @Test
    void testSaleWithCustomer() {
        // Arrange & Act
        Sale sale = new Sale("TERM001", "CASH001");
        sale.setCustomerId("CUST001");

        // Assert
        assertEquals("CUST001", sale.getCustomerId());
    }

    @Test
    void testSaleStatusTransition() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASH001");

        // Act
        sale.setStatus(SaleStatus.FROZEN);

        // Assert
        assertEquals(SaleStatus.FROZEN, sale.getStatus());
    }

    @Test
    void testSaleWithPaymentInfo() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASH001");

        // Act
        sale.setPaymentType("CASH");
        sale.setTransactionId("TXN001");
        sale.setCompletedAt(LocalDateTime.now());

        // Assert
        assertEquals("CASH", sale.getPaymentType());
        assertEquals("TXN001", sale.getTransactionId());
        assertNotNull(sale.getCompletedAt());
    }

    @Test
    void testSaleWithCreditInfo() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASH001");

        // Act
        sale.setPaymentType("CREDIT");
        sale.setCreditReferenceNumber("CREF001");

        // Assert
        assertEquals("CREDIT", sale.getPaymentType());
        assertEquals("CREF001", sale.getCreditReferenceNumber());
    }

    @Test
    void testSaleCancellation() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASH001");

        // Act
        sale.setStatus(SaleStatus.CANCELLED);
        sale.setCancellationReason("Customer changed mind");

        // Assert
        assertEquals(SaleStatus.CANCELLED, sale.getStatus());
        assertEquals("Customer changed mind", sale.getCancellationReason());
    }

    @Test
    void testSaleFreeze() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASH001");
        LocalDateTime frozenTime = LocalDateTime.now();

        // Act
        sale.setStatus(SaleStatus.FROZEN);
        sale.setFrozenAt(frozenTime);

        // Assert
        assertEquals(SaleStatus.FROZEN, sale.getStatus());
        assertEquals(frozenTime, sale.getFrozenAt());
    }
}
