package com.mercato.pos.service;

import com.mercato.pos.dto.ProductDto;
import com.mercato.pos.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private ProductClientService productClientService;
    private static final String BASE_URL = "http://localhost:8081";

    @BeforeEach
    void setUp() {
        productClientService = new ProductClientService(restTemplate, BASE_URL);
    }

    @Test
    void testSearchByNameSuccessful() {
        // Arrange
        ProductDto product1 = new ProductDto("1", "Leche", "123456", BigDecimal.valueOf(2.50), 100, "Lácteos");
        ProductDto product2 = new ProductDto("2", "Leche Descremada", "123457", BigDecimal.valueOf(2.30), 50, "Lácteos");
        ProductDto[] products = {product1, product2};

        when(restTemplate.getForEntity(
                eq(BASE_URL + "/products/search?name=Leche"),
                eq(ProductDto[].class)
        )).thenReturn(new ResponseEntity<>(products, HttpStatus.OK));

        // Act
        List<ProductDto> result = productClientService.searchByName("Leche");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Leche", result.get(0).name());
    }

    @Test
    void testSearchByBarcodeSuccessful() {
        // Arrange
        ProductDto product = new ProductDto("1", "Leche", "123456", BigDecimal.valueOf(2.50), 100, "Lácteos");

        when(restTemplate.getForEntity(
                eq(BASE_URL + "/products/search?barcode=123456"),
                eq(ProductDto.class)
        )).thenReturn(new ResponseEntity<>(product, HttpStatus.OK));

        // Act
        ProductDto result = productClientService.searchByBarcode("123456");

        // Assert
        assertNotNull(result);
        assertEquals("123456", result.barcode());
        assertEquals("Leche", result.name());
    }

    @Test
    void testSearchByNameError4xx() {
        // Arrange
        when(restTemplate.getForEntity(
                anyString(),
                eq(ProductDto[].class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

        // Act & Assert
        assertThrows(HttpClientErrorException.class, () -> {
            productClientService.searchByName("NonExistent");
        });
    }

    @Test
    void testSearchByNameError5xx() {
        // Arrange
        when(restTemplate.getForEntity(
                anyString(),
                eq(ProductDto[].class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"));

        // Act & Assert
        ExternalServiceException exception = assertThrows(ExternalServiceException.class, () -> {
            productClientService.searchByName("Leche");
        });
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void testSearchByNameTimeout() {
        // Arrange
        when(restTemplate.getForEntity(
                anyString(),
                eq(ProductDto[].class)
        )).thenThrow(new ResourceAccessException("Connection timeout"));

        // Act & Assert
        ExternalServiceException exception = assertThrows(ExternalServiceException.class, () -> {
            productClientService.searchByName("Leche");
        });
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void testGetProductSuccessful() {
        // Arrange
        ProductDto product = new ProductDto("1", "Leche", "123456", BigDecimal.valueOf(2.50), 100, "Lácteos");

        when(restTemplate.getForEntity(
                eq(BASE_URL + "/products/1"),
                eq(ProductDto.class)
        )).thenReturn(new ResponseEntity<>(product, HttpStatus.OK));

        // Act
        ProductDto result = productClientService.getProduct("1");

        // Assert
        assertNotNull(result);
        assertEquals("1", result.productId());
    }

    @Test
    void testDecrementStockSuccessful() {
        // Arrange — put() retorna void, se usa doNothing()
        doNothing().when(restTemplate).put(
                eq(BASE_URL + "/products/1/stock/decrement?quantity=5"),
                eq(null)
        );

        // Act & Assert
        assertDoesNotThrow(() -> productClientService.decrementStock("1", 5));
    }

    @Test
    void testIncrementStockSuccessful() {
        // Arrange — put() retorna void, se usa doNothing()
        doNothing().when(restTemplate).put(
                eq(BASE_URL + "/products/1/stock/increment?quantity=5"),
                eq(null)
        );

        // Act & Assert
        assertDoesNotThrow(() -> productClientService.incrementStock("1", 5));
    }

    @Test
    void testDecrementStockError5xx() {
        // Arrange — doThrow para métodos void
        doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"))
                .when(restTemplate).put(anyString(), eq(null));

        // Act & Assert
        ExternalServiceException exception = assertThrows(ExternalServiceException.class, () -> {
            productClientService.decrementStock("1", 5);
        });
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void testDecrementStockTimeout() {
        // Arrange — doThrow para métodos void
        doThrow(new ResourceAccessException("Connection timeout"))
                .when(restTemplate).put(anyString(), eq(null));

        // Act & Assert
        ExternalServiceException exception = assertThrows(ExternalServiceException.class, () -> {
            productClientService.decrementStock("1", 5);
        });
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }
}
