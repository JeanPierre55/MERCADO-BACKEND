package com.mercato.pos.service;

import com.mercato.pos.dto.ProductDto;
import com.mercato.pos.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class ProductClientService {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProductClientService(RestTemplate restTemplate, @Value("${product-api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Search products by name (partial, case-insensitive)
     */
    public List<ProductDto> searchByName(String name) {
        try {
            String url = baseUrl + "/products/search?name=" + name;
            ResponseEntity<ProductDto[]> response = restTemplate.getForEntity(url, ProductDto[].class);
            return Arrays.asList(response.getBody() != null ? response.getBody() : new ProductDto[0]);
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (HttpServerErrorException e) {
            throw new ExternalServiceException("Servicio de productos no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("Servicio de productos no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Search product by barcode (exact match)
     */
    public ProductDto searchByBarcode(String barcode) {
        try {
            String url = baseUrl + "/products/search?barcode=" + barcode;
            ResponseEntity<ProductDto> response = restTemplate.getForEntity(url, ProductDto.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (HttpServerErrorException e) {
            throw new ExternalServiceException("Servicio de productos no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("Servicio de productos no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Get product by ID
     */
    public ProductDto getProduct(String productId) {
        try {
            String url = baseUrl + "/products/" + productId;
            ResponseEntity<ProductDto> response = restTemplate.getForEntity(url, ProductDto.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (HttpServerErrorException e) {
            throw new ExternalServiceException("Servicio de productos no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("Servicio de productos no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Decrement stock for a product
     */
    public void decrementStock(String productId, int quantity) {
        try {
            String url = baseUrl + "/products/" + productId + "/stock/decrement?quantity=" + quantity;
            restTemplate.put(url, null);
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (HttpServerErrorException e) {
            throw new ExternalServiceException("Servicio de productos no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("Servicio de productos no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Increment stock for a product
     */
    public void incrementStock(String productId, int quantity) {
        try {
            String url = baseUrl + "/products/" + productId + "/stock/increment?quantity=" + quantity;
            restTemplate.put(url, null);
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (HttpServerErrorException e) {
            throw new ExternalServiceException("Servicio de productos no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("Servicio de productos no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
