package com.mercato.pos.service;

import com.mercato.pos.dto.AddItemRequest;
import com.mercato.pos.dto.CancelSaleRequest;
import com.mercato.pos.dto.CreateSaleRequest;
import com.mercato.pos.dto.SaleResponse;
import com.mercato.pos.dto.UpdateItemRequest;
import com.mercato.pos.dto.ProductDto;
import com.mercato.pos.exception.InsufficientStockException;
import com.mercato.pos.exception.InvalidQuantityException;
import com.mercato.pos.exception.SaleNotActiveException;
import com.mercato.pos.exception.SaleNotFoundException;
import com.mercato.pos.model.Sale;
import com.mercato.pos.model.SaleItem;
import com.mercato.pos.model.SaleStatus;
import com.mercato.pos.repository.SaleItemRepository;
import com.mercato.pos.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@DisplayName("SaleService Tests")
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private ProductClientService productClientService;

    private SaleService saleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        saleService = new SaleService(saleRepository, saleItemRepository, productClientService);
    }

    @Test
    @DisplayName("Should create a new sale successfully")
    void testCreateSaleSuccessful() {
        // Arrange
        CreateSaleRequest request = new CreateSaleRequest("TERM001", null);
        Sale expectedSale = new Sale("TERM001", "cashier1");
        expectedSale.setStatus(SaleStatus.ACTIVE);

        when(saleRepository.save(any(Sale.class))).thenReturn(expectedSale);

        // Act
        SaleResponse response = saleService.createSale(request, "cashier1");

        // Assert
        assertNotNull(response);
        assertEquals("TERM001", response.terminalId());
        assertEquals("cashier1", response.cashierId());
        assertEquals("ACTIVE", response.status());
        assertEquals(BigDecimal.ZERO, response.subtotal());
        assertEquals(BigDecimal.ZERO, response.total());
        verify(saleRepository, times(1)).save(any(Sale.class));
    }

    @Test
    @DisplayName("Should create a sale with customer ID")
    void testCreateSaleWithCustomerId() {
        // Arrange
        CreateSaleRequest request = new CreateSaleRequest("TERM001", "CUST001");
        Sale expectedSale = new Sale("TERM001", "cashier1");
        expectedSale.setCustomerId("CUST001");
        expectedSale.setStatus(SaleStatus.ACTIVE);

        when(saleRepository.save(any(Sale.class))).thenReturn(expectedSale);

        // Act
        SaleResponse response = saleService.createSale(request, "cashier1");

        // Assert
        assertNotNull(response);
        assertEquals("CUST001", response.customerId());
        verify(saleRepository, times(1)).save(any(Sale.class));
    }

    @Test
    @DisplayName("Should get a sale by ID")
    void testGetSaleSuccessful() {
        // Arrange
        Sale sale = new Sale("TERM001", "cashier1");
        sale.setId("SALE001");

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));

        // Act
        SaleResponse response = saleService.getSale("SALE001");

        // Assert
        assertNotNull(response);
        assertEquals("SALE001", response.id());
        assertEquals("TERM001", response.terminalId());
    }

    @Test
    @DisplayName("Should throw SaleNotFoundException when sale does not exist")
    void testGetSaleNotFound() {
        // Arrange
        when(saleRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(SaleNotFoundException.class, () -> saleService.getSale("NONEXISTENT"));
    }

    @Test
    @DisplayName("Should add a new item to a sale successfully")
    void testAddItemNewItemSuccessful() {
        // Arrange
        Sale sale = new Sale("TERM001", "cashier1");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);

        ProductDto product = new ProductDto("PROD001", "Product 1", "123456", 
                                            new BigDecimal("10.00"), 100, "Category1");

        AddItemRequest request = new AddItemRequest("PROD001", "123456", 2);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(productClientService.getProduct("PROD001")).thenReturn(product);
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        // Act
        SaleResponse response = saleService.addItem("SALE001", request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals("PROD001", response.items().get(0).productId());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(new BigDecimal("20.00"), response.items().get(0).lineTotal());
        verify(saleRepository, times(1)).save(any(Sale.class));
    }

    @Test
    @DisplayName("Should increment quantity when adding duplicate product")
    void testAddItemDuplicateProductIncrementsQuantity() {
        // Arrange
        Sale sale = new Sale("TERM001", "cashier1");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);

        SaleItem existingItem = new SaleItem("PROD001", "Product 1", "123456",
                                             new BigDecimal("10.00"), 1000, 2);
        existingItem.setId("ITEM001");
        existingItem.setLineTotalCents(2000);
        existingItem.setLineTotal(new BigDecimal("20.00"));
        existingItem.setSale(sale);
        sale.getItems().add(existingItem);

        ProductDto product = new ProductDto("PROD001", "Product 1", "123456",
                                            new BigDecimal("10.00"), 100, "Category1");

        AddItemRequest request = new AddItemRequest("PROD001", "123456", 3);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(productClientService.getProduct("PROD001")).thenReturn(product);
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        // Act
        SaleResponse response = saleService.addItem("SALE001", request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals(5, response.items().get(0).quantity()); // 2 + 3
        verify(saleRepository, times(1)).save(any(Sale.class));
    }

    @Test
    @DisplayName("Should throw SaleNotActiveException when adding item to non-active sale")
    void testAddItemToNonActiveSale() {
        // Arrange
        Sale sale = new Sale("TERM001", "cashier1");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.CANCELLED);

        AddItemRequest request = new AddItemRequest("PROD001", "123456", 1);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));

        // Act & Assert
        assertThrows(SaleNotActiveException.class, () -> saleService.addItem("SALE001", request));
    }

    @Test
    @DisplayName("Should throw InvalidQuantityException when quantity is less than 1")
    void testAddItemInvalidQuantity() {
        // Arrange
        Sale sale = new Sale("TERM001", "cashier1");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);

        AddItemRequest request = new AddItemRequest("PROD001", "123456", 0);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));

        // Act & Assert
        assertThrows(InvalidQuantityException.class, () -> saleService.addItem("SALE001", request));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when stock is insufficient")
    void testAddItemInsufficientStock() {
        // Arrange
        Sale sale = new Sale("TERM001", "cashier1");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);

        ProductDto product = new ProductDto("PROD001", "Product 1", "123456",
                                            new BigDecimal("10.00"), 5, "Category1");

        AddItemRequest request = new AddItemRequest("PROD001", "123456", 10);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(productClientService.getProduct("PROD001")).thenReturn(product);

        // Act & Assert
        assertThrows(InsufficientStockException.class, () -> saleService.addItem("SALE001", request));
    }

    @Test
    @DisplayName("Should update item quantity successfully")
    void testUpdateItemSuccessful() {
        // Arrange
        Sale sale = new Sale("TERM001", "cashier1");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);

        SaleItem item = new SaleItem("PROD001", "Product 1", "123456",
                                     new BigDecimal("10.00"), 1000, 2);
        item.setId("ITEM001");
        item.setLineTotalCents(2000);
        item.setLineTotal(new BigDecimal("20.00"));
        item.setSale(sale);
        sale.getItems().add(item);

        ProductDto product = new ProductDto("PROD001", "Product 1", "123456",
                                            new BigDecimal("10.00"), 100, "Category1");

        UpdateItemRequest request = new UpdateItemRequest(5);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(productClientService.getProduct("PROD001")).thenReturn(product);
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        // Act
        SaleResponse response = saleService.updateItem("SALE001", "ITEM001", request);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.items().get(0).quantity());
        verify(saleRepository, times(1)).save(any(Sale.class));
    }

    @Test
    @DisplayName("Should throw SaleNotActiveException when updating item in non-active sale")
    void testUpdateItemNonActiveSale() {
        // Arrange
        Sale sale = new Sale("TERM001", "cashier1");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.COMPLETED);

        UpdateItemRequest request = new UpdateItemRequest(5);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));

        // Act & Assert
        assertThrows(SaleNotActiveException.class, () -> saleService.updateItem("SALE001", "ITEM001", request));
    }

    @Test
    @DisplayName("Should remove item from sale successfully")
    void testRemoveItemSuccessful() {
        // Arrange
        Sale sale = new Sale("TERM001", "cashier1");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);

        SaleItem item = new SaleItem("PROD001", "Product 1", "123456",
                                     new BigDecimal("10.00"), 1000, 2);
        item.setId("ITEM001");
        item.setLineTotalCents(2000);
        item.setLineTotal(new BigDecimal("20.00"));
        item.setSale(sale);
        sale.getItems().add(item);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        // Act
        SaleResponse response = saleService.removeItem("SALE001", "ITEM001");

        // Assert
        assertNotNull(response);
        assertEquals(0, response.items().size());
        verify(saleRepository, times(1)).save(any(Sale.class));
    }

    @Test
    @DisplayName("Should throw SaleNotActiveException when removing item from non-active sale")
    void testRemoveItemNonActiveSale() {
        // Arrange
        Sale sale = new Sale("TERM001", "cashier1");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.CANCELLED);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));

        // Act & Assert
        assertThrows(SaleNotActiveException.class, () -> saleService.removeItem("SALE001", "ITEM001"));
    }

    @Test
    @DisplayName("Should cancel sale successfully")
    void testCancelSaleSuccessful() {
        // Arrange
        Sale sale = new Sale("TERM001", "cashier1");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);

        CancelSaleRequest request = new CancelSaleRequest("Customer changed mind");

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        // Act
        SaleResponse response = saleService.cancelSale("SALE001", request);

        // Assert
        assertNotNull(response);
        assertEquals("CANCELLED", response.status());
        verify(saleRepository, times(1)).save(any(Sale.class));
    }

    @Test
    @DisplayName("Should cancel frozen sale successfully")
    void testCancelFrozenSaleSuccessful() {
        // Arrange
        Sale sale = new Sale("TERM001", "cashier1");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.FROZEN);

        CancelSaleRequest request = new CancelSaleRequest("Cancelling frozen sale");

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        // Act
        SaleResponse response = saleService.cancelSale("SALE001", request);

        // Assert
        assertNotNull(response);
        assertEquals("CANCELLED", response.status());
        verify(saleRepository, times(1)).save(any(Sale.class));
    }

    @Test
    @DisplayName("Should throw SaleNotActiveException when cancelling completed sale")
    void testCancelCompletedSale() {
        // Arrange
        Sale sale = new Sale("TERM001", "cashier1");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.COMPLETED);

        CancelSaleRequest request = new CancelSaleRequest("Cannot cancel");

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));

        // Act & Assert
        assertThrows(SaleNotActiveException.class, () -> saleService.cancelSale("SALE001", request));
    }
}
