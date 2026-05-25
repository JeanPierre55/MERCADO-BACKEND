package com.mercato.pos.service;

import com.mercato.pos.dto.CustomerDto;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private CustomerClientService customerClientService;
    private static final String BASE_URL = "http://localhost:8082";

    @BeforeEach
    void setUp() {
        customerClientService = new CustomerClientService(restTemplate, BASE_URL);
    }

    @Test
    void testSearchByNameSuccessful() {
        // Arrange
        CustomerDto customer1 = new CustomerDto("1", "Juan Pérez", "CC", "123456789", "APPROVED");
        CustomerDto customer2 = new CustomerDto("2", "Juan García", "CC", "987654321", "PENDING");
        CustomerDto[] customers = {customer1, customer2};

        when(restTemplate.getForEntity(
                eq(BASE_URL + "/customers/search?name=Juan"),
                eq(CustomerDto[].class)
        )).thenReturn(new ResponseEntity<>(customers, HttpStatus.OK));

        // Act
        List<CustomerDto> result = customerClientService.searchByName("Juan");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Juan Pérez", result.get(0).fullName());
    }

    @Test
    void testSearchByDocumentSuccessful() {
        // Arrange
        CustomerDto customer = new CustomerDto("1", "Juan Pérez", "CC", "123456789", "APPROVED");

        when(restTemplate.getForEntity(
                eq(BASE_URL + "/customers/search?document=123456789"),
                eq(CustomerDto.class)
        )).thenReturn(new ResponseEntity<>(customer, HttpStatus.OK));

        // Act
        CustomerDto result = customerClientService.searchByDocument("123456789");

        // Assert
        assertNotNull(result);
        assertEquals("123456789", result.documentNumber());
        assertEquals("Juan Pérez", result.fullName());
    }

    @Test
    void testSearchByNameError4xx() {
        // Arrange
        when(restTemplate.getForEntity(
                anyString(),
                eq(CustomerDto[].class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

        // Act & Assert
        assertThrows(HttpClientErrorException.class, () -> {
            customerClientService.searchByName("NonExistent");
        });
    }

    @Test
    void testSearchByNameError5xx() {
        // Arrange
        when(restTemplate.getForEntity(
                anyString(),
                eq(CustomerDto[].class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"));

        // Act & Assert
        ExternalServiceException exception = assertThrows(ExternalServiceException.class, () -> {
            customerClientService.searchByName("Juan");
        });
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void testSearchByNameTimeout() {
        // Arrange
        when(restTemplate.getForEntity(
                anyString(),
                eq(CustomerDto[].class)
        )).thenThrow(new ResourceAccessException("Connection timeout"));

        // Act & Assert
        ExternalServiceException exception = assertThrows(ExternalServiceException.class, () -> {
            customerClientService.searchByName("Juan");
        });
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void testGetCustomerSuccessful() {
        // Arrange
        CustomerDto customer = new CustomerDto("1", "Juan Pérez", "CC", "123456789", "APPROVED");

        when(restTemplate.getForEntity(
                eq(BASE_URL + "/customers/1"),
                eq(CustomerDto.class)
        )).thenReturn(new ResponseEntity<>(customer, HttpStatus.OK));

        // Act
        CustomerDto result = customerClientService.getCustomer("1");

        // Assert
        assertNotNull(result);
        assertEquals("1", result.customerId());
    }

    @Test
    void testSearchByDocumentError4xx() {
        // Arrange
        when(restTemplate.getForEntity(
                anyString(),
                eq(CustomerDto.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

        // Act & Assert
        assertThrows(HttpClientErrorException.class, () -> {
            customerClientService.searchByDocument("999999999");
        });
    }

    @Test
    void testSearchByDocumentError5xx() {
        // Arrange
        when(restTemplate.getForEntity(
                anyString(),
                eq(CustomerDto.class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"));

        // Act & Assert
        ExternalServiceException exception = assertThrows(ExternalServiceException.class, () -> {
            customerClientService.searchByDocument("123456789");
        });
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void testSearchByDocumentTimeout() {
        // Arrange
        when(restTemplate.getForEntity(
                anyString(),
                eq(CustomerDto.class)
        )).thenThrow(new ResourceAccessException("Connection timeout"));

        // Act & Assert
        ExternalServiceException exception = assertThrows(ExternalServiceException.class, () -> {
            customerClientService.searchByDocument("123456789");
        });
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void testGetCustomerError5xx() {
        // Arrange
        when(restTemplate.getForEntity(
                anyString(),
                eq(CustomerDto.class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"));

        // Act & Assert
        ExternalServiceException exception = assertThrows(ExternalServiceException.class, () -> {
            customerClientService.getCustomer("1");
        });
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void testGetCustomerTimeout() {
        // Arrange
        when(restTemplate.getForEntity(
                anyString(),
                eq(CustomerDto.class)
        )).thenThrow(new ResourceAccessException("Connection timeout"));

        // Act & Assert
        ExternalServiceException exception = assertThrows(ExternalServiceException.class, () -> {
            customerClientService.getCustomer("1");
        });
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }
}
