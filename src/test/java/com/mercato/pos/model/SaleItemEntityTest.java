package com.mercato.pos.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Tests unitarios para la entidad SaleItem.
 */
class SaleItemEntityTest {

    @Test
    void testSaleItemCreation() {
        // Arrange & Act
        SaleItem item = new SaleItem("PROD001", "Product 1", "123456",
                                     new BigDecimal("10.00"), 1000L, 2);

        // Assert
        assertNotNull(item.getId());
        assertEquals("PROD001", item.getProductId());
        assertEquals("Product 1", item.getProductName());
        assertEquals("123456", item.getBarcode());
        assertEquals(new BigDecimal("10.00"), item.getUnitPrice());
        assertEquals(1000L, item.getUnitPriceCents());
        assertEquals(2, item.getQuantity());
    }

    @Test
    void testSaleItemLineTotal() {
        // Arrange
        SaleItem item = new SaleItem("PROD001", "Product 1", "123456",
                                     new BigDecimal("10.00"), 1000L, 2);

        // Act
        item.setLineTotal(new BigDecimal("20.00"));
        item.setLineTotalCents(2000L);

        // Assert
        assertEquals(new BigDecimal("20.00"), item.getLineTotal());
        assertEquals(2000L, item.getLineTotalCents());
    }

    @Test
    void testSaleItemWithSale() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASH001");
        SaleItem item = new SaleItem("PROD001", "Product 1", "123456",
                                     new BigDecimal("10.00"), 1000L, 2);

        // Act
        item.setSale(sale);
        sale.getItems().add(item);

        // Assert
        assertEquals(sale, item.getSale());
        assertTrue(sale.getItems().contains(item));
    }

    @Test
    void testSaleItemQuantityUpdate() {
        // Arrange
        SaleItem item = new SaleItem("PROD001", "Product 1", "123456",
                                     new BigDecimal("10.00"), 1000L, 2);

        // Act
        item.setQuantity(5);

        // Assert
        assertEquals(5, item.getQuantity());
    }
}
