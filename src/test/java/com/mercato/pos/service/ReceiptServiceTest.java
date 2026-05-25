package com.mercato.pos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercato.pos.dto.ReceiptResponse;
import com.mercato.pos.exception.ReceiptNotFoundException;
import com.mercato.pos.model.Receipt;
import com.mercato.pos.model.Sale;
import com.mercato.pos.model.SaleItem;
import com.mercato.pos.repository.ReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReceiptServiceTest {

    private ReceiptService receiptService;

    @Mock
    private ReceiptRepository receiptRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        receiptService = new ReceiptService(receiptRepository, objectMapper);
    }

    @Test
    void testGenerateSaleReceiptCash() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setSubtotalCents(10000);
        sale.setTaxCents(1900);
        sale.setTotalCents(11900);
        sale.setPaymentType("CASH");

        SaleItem item = new SaleItem("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10000, 1);
        item.setId("ITEM001");
        sale.getItems().add(item);

        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Receipt receipt = receiptService.generateSaleReceipt(sale, BigDecimal.valueOf(119.00));

        // Assert
        assertNotNull(receipt);
        assertNotNull(receipt.getTransactionId());
        assertEquals("SALE", receipt.getReceiptType());
        assertEquals("SALE001", receipt.getSaleId());
        verify(receiptRepository, times(1)).save(any(Receipt.class));
    }

    @Test
    void testGenerateSaleReceiptCredit() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setCustomerId("CUST001");
        sale.setSubtotalCents(10000);
        sale.setTaxCents(1900);
        sale.setTotalCents(11900);
        sale.setPaymentType("CREDIT");
        sale.setCreditReferenceNumber("CR-12345678");

        SaleItem item = new SaleItem("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10000, 1);
        item.setId("ITEM001");
        sale.getItems().add(item);

        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Receipt receipt = receiptService.generateSaleReceipt(sale, null);

        // Assert
        assertNotNull(receipt);
        assertNotNull(receipt.getTransactionId());
        assertEquals("SALE", receipt.getReceiptType());
        verify(receiptRepository, times(1)).save(any(Receipt.class));
    }

    @Test
    void testGenerateReturnReceipt() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setTransactionId("TXN001");
        sale.setSubtotalCents(10000);
        sale.setTaxCents(1900);
        sale.setTotalCents(11900);

        SaleItem item = new SaleItem("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10000, 1);
        item.setId("ITEM001");
        sale.getItems().add(item);

        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Receipt receipt = receiptService.generateReturnReceipt(sale, "Defective product", "RETURN");

        // Assert
        assertNotNull(receipt);
        assertNotNull(receipt.getTransactionId());
        assertEquals("RETURN", receipt.getReceiptType());
        assertEquals("TXN001", receipt.getOriginalTransactionId());
        verify(receiptRepository, times(1)).save(any(Receipt.class));
    }

    @Test
    void testGetReceiptSuccess() {
        // Arrange
        Receipt receipt = new Receipt();
        receipt.setId("REC001");
        receipt.setTransactionId("TXN001");
        receipt.setReceiptType("SALE");
        receipt.setReceiptJson("{\"transactionId\": \"TXN001\"}");

        when(receiptRepository.findByTransactionId("TXN001")).thenReturn(Optional.of(receipt));

        // Act
        ReceiptResponse response = receiptService.getReceipt("TXN001");

        // Assert
        assertNotNull(response);
        assertEquals("TXN001", response.transactionId());
        assertEquals("SALE", response.receiptType());
        verify(receiptRepository, times(1)).findByTransactionId("TXN001");
    }

    @Test
    void testGetReceiptNotFound() {
        // Arrange
        when(receiptRepository.findByTransactionId("TXN001")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReceiptNotFoundException.class, () -> receiptService.getReceipt("TXN001"));
    }
}
