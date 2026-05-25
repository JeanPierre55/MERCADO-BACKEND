package com.mercato.pos.service;

import com.mercato.pos.dto.CheckoutRequest;
import com.mercato.pos.dto.CustomerDto;
import com.mercato.pos.dto.ProductDto;
import com.mercato.pos.dto.ReceiptResponse;
import com.mercato.pos.exception.CreditNotApprovedException;
import com.mercato.pos.exception.CustomerRequiredException;
import com.mercato.pos.exception.EmptySaleException;
import com.mercato.pos.exception.InsufficientPaymentException;
import com.mercato.pos.exception.InsufficientStockException;
import com.mercato.pos.exception.SaleNotFoundException;
import com.mercato.pos.model.Receipt;
import com.mercato.pos.model.Sale;
import com.mercato.pos.model.SaleItem;
import com.mercato.pos.model.SaleStatus;
import com.mercato.pos.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private PaymentService paymentService;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductClientService productClientService;

    @Mock
    private CustomerClientService customerClientService;

    @Mock
    private ReceiptService receiptService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentService = new PaymentService(saleRepository, productClientService, customerClientService, receiptService);
    }

    @Test
    void testCheckoutCashSuccess() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setSubtotalCents(10000);
        sale.setTaxCents(1900);
        sale.setTotalCents(11900);

        SaleItem item = new SaleItem("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10000, 1);
        item.setId("ITEM001");
        item.setLineTotalCents(10000);
        sale.getItems().add(item);

        ProductDto product = new ProductDto("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10, "Category");

        Receipt receipt = new Receipt();
        receipt.setTransactionId("TXN001");
        receipt.setReceiptType("SALE");

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(productClientService.getProduct("PROD001")).thenReturn(product);
        when(receiptService.generateSaleReceipt(any(Sale.class), any(BigDecimal.class))).thenReturn(receipt);
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        CheckoutRequest request = new CheckoutRequest("CASH", BigDecimal.valueOf(119.00));

        // Act
        ReceiptResponse response = paymentService.checkout("SALE001", request);

        // Assert
        assertNotNull(response);
        assertEquals("TXN001", response.transactionId());
        verify(saleRepository, times(1)).findById("SALE001");
        verify(productClientService, times(1)).getProduct("PROD001");
        verify(productClientService, times(1)).decrementStock("PROD001", 1);
    }

    @Test
    void testCheckoutCashEmptySale() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));

        CheckoutRequest request = new CheckoutRequest("CASH", BigDecimal.valueOf(100));

        // Act & Assert
        assertThrows(EmptySaleException.class, () -> paymentService.checkout("SALE001", request));
    }

    @Test
    void testCheckoutCashInsufficientPayment() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setTotalCents(11900);

        SaleItem item = new SaleItem("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10000, 1);
        item.setId("ITEM001");
        sale.getItems().add(item);

        ProductDto product = new ProductDto("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10, "Category");

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(productClientService.getProduct("PROD001")).thenReturn(product);

        CheckoutRequest request = new CheckoutRequest("CASH", BigDecimal.valueOf(50));

        // Act & Assert
        assertThrows(InsufficientPaymentException.class, () -> paymentService.checkout("SALE001", request));
    }

    @Test
    void testCheckoutCreditSuccess() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setCustomerId("CUST001");
        sale.setSubtotalCents(10000);
        sale.setTaxCents(1900);
        sale.setTotalCents(11900);

        SaleItem item = new SaleItem("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10000, 1);
        item.setId("ITEM001");
        sale.getItems().add(item);

        ProductDto product = new ProductDto("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10, "Category");
        CustomerDto customer = new CustomerDto("CUST001", "John Doe", "CC", "123456", "APPROVED");

        Receipt receipt = new Receipt();
        receipt.setTransactionId("TXN001");
        receipt.setReceiptType("SALE");

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(productClientService.getProduct("PROD001")).thenReturn(product);
        when(customerClientService.getCustomer("CUST001")).thenReturn(customer);
        when(receiptService.generateSaleReceipt(any(Sale.class), any())).thenReturn(receipt);
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        CheckoutRequest request = new CheckoutRequest("CREDIT", BigDecimal.ZERO);

        // Act
        ReceiptResponse response = paymentService.checkout("SALE001", request);

        // Assert
        assertNotNull(response);
        assertEquals("TXN001", response.transactionId());
        verify(saleRepository, times(1)).findById("SALE001");
        verify(productClientService, times(1)).decrementStock("PROD001", 1);
    }

    @Test
    void testCheckoutCreditNoCustomer() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);

        SaleItem item = new SaleItem("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10000, 1);
        item.setId("ITEM001");
        sale.getItems().add(item);

        ProductDto product = new ProductDto("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10, "Category");

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(productClientService.getProduct("PROD001")).thenReturn(product);

        CheckoutRequest request = new CheckoutRequest("CREDIT", BigDecimal.ZERO);

        // Act & Assert
        assertThrows(CustomerRequiredException.class, () -> paymentService.checkout("SALE001", request));
    }

    @Test
    void testCheckoutCreditNotApproved() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setCustomerId("CUST001");

        SaleItem item = new SaleItem("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10000, 1);
        item.setId("ITEM001");
        sale.getItems().add(item);

        ProductDto product = new ProductDto("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10, "Category");
        CustomerDto customer = new CustomerDto("CUST001", "John Doe", "CC", "123456", "REJECTED");

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(productClientService.getProduct("PROD001")).thenReturn(product);
        when(customerClientService.getCustomer("CUST001")).thenReturn(customer);

        CheckoutRequest request = new CheckoutRequest("CREDIT", BigDecimal.ZERO);

        // Act & Assert
        assertThrows(CreditNotApprovedException.class, () -> paymentService.checkout("SALE001", request));
    }

    @Test
    void testCheckoutSaleNotFound() {
        // Arrange
        when(saleRepository.findById("SALE001")).thenReturn(Optional.empty());

        CheckoutRequest request = new CheckoutRequest("CASH", BigDecimal.valueOf(100));

        // Act & Assert
        assertThrows(SaleNotFoundException.class, () -> paymentService.checkout("SALE001", request));
    }
}
