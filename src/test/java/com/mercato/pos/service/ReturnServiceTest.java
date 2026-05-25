package com.mercato.pos.service;

import com.mercato.pos.dto.PartialReturnRequest;
import com.mercato.pos.dto.ReceiptResponse;
import com.mercato.pos.dto.ReturnItemRequest;
import com.mercato.pos.dto.ReturnRequest;
import com.mercato.pos.exception.SaleAlreadyReturnedException;
import com.mercato.pos.exception.SaleNotCompletedException;
import com.mercato.pos.exception.SaleNotFoundException;
import com.mercato.pos.model.Receipt;
import com.mercato.pos.model.Sale;
import com.mercato.pos.model.SaleItem;
import com.mercato.pos.model.SaleStatus;
import com.mercato.pos.repository.ReturnRecordRepository;
import com.mercato.pos.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReturnServiceTest {

    private ReturnService returnService;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ReturnRecordRepository returnRecordRepository;

    @Mock
    private ProductClientService productClientService;

    @Mock
    private ReceiptService receiptService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        returnService = new ReturnService(saleRepository, returnRecordRepository, productClientService, receiptService);
    }

    @Test
    void testFullReturnSuccess() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setTransactionId("TXN001");

        SaleItem item = new SaleItem("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10000, 1);
        item.setId("ITEM001");
        sale.getItems().add(item);

        Receipt receipt = new Receipt();
        receipt.setTransactionId("TXN002");
        receipt.setReceiptType("RETURN");

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(receiptService.generateReturnReceipt(any(Sale.class), any(String.class), any(String.class)))
            .thenReturn(receipt);
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        ReturnRequest request = new ReturnRequest("Defective product");

        // Act
        ReceiptResponse response = returnService.fullReturn("SALE001", request);

        // Assert
        assertNotNull(response);
        assertEquals("TXN002", response.transactionId());
        verify(saleRepository, times(1)).findById("SALE001");
        verify(productClientService, times(1)).incrementStock("PROD001", 1);
        verify(receiptService, times(1)).generateReturnReceipt(any(Sale.class), any(String.class), any(String.class));
    }

    @Test
    void testFullReturnNotCompleted() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));

        ReturnRequest request = new ReturnRequest("Defective product");

        // Act & Assert
        assertThrows(SaleNotCompletedException.class, () -> returnService.fullReturn("SALE001", request));
    }

    @Test
    void testFullReturnSaleNotFound() {
        // Arrange
        when(saleRepository.findById("SALE001")).thenReturn(Optional.empty());

        ReturnRequest request = new ReturnRequest("Defective product");

        // Act & Assert
        assertThrows(SaleNotFoundException.class, () -> returnService.fullReturn("SALE001", request));
    }

    @Test
    void testPartialReturnSuccess() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setTransactionId("TXN001");

        SaleItem item = new SaleItem("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10000, 2);
        item.setId("ITEM001");
        sale.getItems().add(item);

        Receipt receipt = new Receipt();
        receipt.setTransactionId("TXN002");
        receipt.setReceiptType("PARTIAL_RETURN");

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(returnRecordRepository.findBySaleIdAndProductId("SALE001", "PROD001")).thenReturn(List.of());
        when(receiptService.generateReturnReceipt(any(Sale.class), any(String.class), any(String.class)))
            .thenReturn(receipt);
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        ReturnItemRequest returnItem = new ReturnItemRequest("PROD001", 1, "Defective");
        PartialReturnRequest request = new PartialReturnRequest(List.of(returnItem));

        // Act
        ReceiptResponse response = returnService.partialReturn("SALE001", request);

        // Assert
        assertNotNull(response);
        assertEquals("TXN002", response.transactionId());
        verify(saleRepository, times(1)).findById("SALE001");
        verify(productClientService, times(1)).incrementStock("PROD001", 1);
    }

    @Test
    void testPartialReturnExceedsQuantity() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.COMPLETED);

        SaleItem item = new SaleItem("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10000, 1);
        item.setId("ITEM001");
        sale.getItems().add(item);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(returnRecordRepository.findBySaleIdAndProductId("SALE001", "PROD001")).thenReturn(List.of());

        ReturnItemRequest returnItem = new ReturnItemRequest("PROD001", 2, "Defective");
        PartialReturnRequest request = new PartialReturnRequest(List.of(returnItem));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> returnService.partialReturn("SALE001", request));
    }

    @Test
    void testPartialReturnNotCompleted() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));

        ReturnItemRequest returnItem = new ReturnItemRequest("PROD001", 1, "Defective");
        PartialReturnRequest request = new PartialReturnRequest(List.of(returnItem));

        // Act & Assert
        assertThrows(SaleNotCompletedException.class, () -> returnService.partialReturn("SALE001", request));
    }
}
