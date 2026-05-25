package com.mercato.pos.util;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.mercato.pos.model.Sale;
import com.mercato.pos.model.SaleItem;

/**
 * Tests unitarios para MoneyCalculator.
 */
class MoneyCalculatorTest {

    @Test
    void testToCents_ValidAmount() {
        // Arrange
        BigDecimal amount = new BigDecimal("10.50");

        // Act
        long cents = MoneyCalculator.toCents(amount);

        // Assert
        assertEquals(1050L, cents);
    }

    @Test
    void testToCents_ZeroAmount() {
        // Arrange
        BigDecimal amount = BigDecimal.ZERO;

        // Act
        long cents = MoneyCalculator.toCents(amount);

        // Assert
        assertEquals(0L, cents);
    }

    @Test
    void testToCents_NullAmount() {
        // Act
        long cents = MoneyCalculator.toCents(null);

        // Assert
        assertEquals(0L, cents);
    }

    @Test
    void testToCents_NegativeAmount() {
        // Arrange
        BigDecimal amount = new BigDecimal("-10.50");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> MoneyCalculator.toCents(amount));
    }

    @Test
    void testFromCents_ValidCents() {
        // Arrange
        long cents = 1050L;

        // Act
        BigDecimal amount = MoneyCalculator.fromCents(cents);

        // Assert
        assertEquals(new BigDecimal("10.50"), amount);
        assertEquals(2, amount.scale());
    }

    @Test
    void testFromCents_ZeroCents() {
        // Arrange
        long cents = 0L;

        // Act
        BigDecimal amount = MoneyCalculator.fromCents(cents);

        // Assert
        assertEquals(BigDecimal.ZERO.setScale(2), amount);
    }

    @Test
    void testFromCents_NegativeCents() {
        // Arrange
        long cents = -1050L;

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> MoneyCalculator.fromCents(cents));
    }

    @Test
    void testRoundTripConversion() {
        // Arrange
        long originalCents = 1050L;

        // Act
        BigDecimal converted = MoneyCalculator.fromCents(originalCents);
        long backToCents = MoneyCalculator.toCents(converted);

        // Assert
        assertEquals(originalCents, backToCents);
    }

    @Test
    void testRecalculateTotals_SimpleCase() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASH001");
        SaleItem item1 = new SaleItem("PROD001", "Product 1", "123456", 
                                      new BigDecimal("10.00"), 1000L, 2);
        item1.setLineTotal(new BigDecimal("20.00"));
        item1.setLineTotalCents(2000L);
        sale.getItems().add(item1);
        item1.setSale(sale);

        // Act
        MoneyCalculator.recalculateTotals(sale);

        // Assert
        assertEquals(2000L, sale.getSubtotalCents()); // 10.00 * 2 = 20.00
        assertEquals(380L, sale.getTaxCents());       // 20.00 * 0.19 = 3.80
        assertEquals(2380L, sale.getTotalCents());    // 20.00 + 3.80 = 23.80
    }

    @Test
    void testRecalculateTotals_WithDiscount() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASH001");
        sale.setDiscountCents(500L); // $5.00 discount
        SaleItem item1 = new SaleItem("PROD001", "Product 1", "123456", 
                                      new BigDecimal("10.00"), 1000L, 2);
        item1.setLineTotal(new BigDecimal("20.00"));
        item1.setLineTotalCents(2000L);
        sale.getItems().add(item1);
        item1.setSale(sale);

        // Act
        MoneyCalculator.recalculateTotals(sale);

        // Assert
        assertEquals(2000L, sale.getSubtotalCents());
        assertEquals(380L, sale.getTaxCents());
        assertEquals(1880L, sale.getTotalCents()); // 20.00 + 3.80 - 5.00 = 18.80
    }

    @Test
    void testRecalculateTotals_MultipleItems() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASH001");
        SaleItem item1 = new SaleItem("PROD001", "Product 1", "123456", 
                                      new BigDecimal("10.00"), 1000L, 2);
        item1.setLineTotal(new BigDecimal("20.00"));
        item1.setLineTotalCents(2000L);
        SaleItem item2 = new SaleItem("PROD002", "Product 2", "654321", 
                                      new BigDecimal("5.00"), 500L, 3);
        item2.setLineTotal(new BigDecimal("15.00"));
        item2.setLineTotalCents(1500L);
        sale.getItems().add(item1);
        sale.getItems().add(item2);
        item1.setSale(sale);
        item2.setSale(sale);

        // Act
        MoneyCalculator.recalculateTotals(sale);

        // Assert
        assertEquals(3500L, sale.getSubtotalCents()); // 20.00 + 15.00 = 35.00
        assertEquals(665L, sale.getTaxCents());       // 35.00 * 0.19 = 6.65
        assertEquals(4165L, sale.getTotalCents());    // 35.00 + 6.65 = 41.65
    }

    @Test
    void testRecalculateTotals_NullSale() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> MoneyCalculator.recalculateTotals(null));
    }

    @Test
    void testRecalculateTotals_NegativeDiscount() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASH001");
        sale.setDiscountCents(-100L);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> MoneyCalculator.recalculateTotals(sale));
    }

    @Test
    void testRecalculateTotals_CustomTaxRate() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASH001");
        sale.setTaxRate(new BigDecimal("0.10")); // 10% tax
        SaleItem item1 = new SaleItem("PROD001", "Product 1", "123456", 
                                      new BigDecimal("10.00"), 1000L, 1);
        item1.setLineTotal(new BigDecimal("10.00"));
        item1.setLineTotalCents(1000L);
        sale.getItems().add(item1);
        item1.setSale(sale);

        // Act
        MoneyCalculator.recalculateTotals(sale);

        // Assert
        assertEquals(1000L, sale.getSubtotalCents());
        assertEquals(100L, sale.getTaxCents());  // 10.00 * 0.10 = 1.00
        assertEquals(1100L, sale.getTotalCents());
    }

    @Test
    void testRecalculateTotals_EmptyItems() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASH001");
        sale.setItems(new ArrayList<>());

        // Act
        MoneyCalculator.recalculateTotals(sale);

        // Assert
        assertEquals(0L, sale.getSubtotalCents());
        assertEquals(0L, sale.getTaxCents());
        assertEquals(0L, sale.getTotalCents());
    }
}
