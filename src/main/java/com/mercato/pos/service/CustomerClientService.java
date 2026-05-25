package com.mercato.pos.service;

import com.mercato.pos.dto.CustomerDto;
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
public class CustomerClientService {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CustomerClientService(RestTemplate restTemplate, @Value("${customer-api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Search customers by name (partial, case-insensitive)
     */
    public List<CustomerDto> searchByName(String name) {
        try {
            String url = baseUrl + "/customers/search?name=" + name;
            ResponseEntity<CustomerDto[]> response = restTemplate.getForEntity(url, CustomerDto[].class);
            return Arrays.asList(response.getBody() != null ? response.getBody() : new CustomerDto[0]);
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (HttpServerErrorException e) {
            throw new ExternalServiceException("Servicio de clientes no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("Servicio de clientes no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Search customer by document (exact match)
     */
    public CustomerDto searchByDocument(String document) {
        try {
            String url = baseUrl + "/customers/search?document=" + document;
            ResponseEntity<CustomerDto> response = restTemplate.getForEntity(url, CustomerDto.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (HttpServerErrorException e) {
            throw new ExternalServiceException("Servicio de clientes no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("Servicio de clientes no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Get customer by ID
     */
    public CustomerDto getCustomer(String customerId) {
        try {
            String url = baseUrl + "/customers/" + customerId;
            ResponseEntity<CustomerDto> response = restTemplate.getForEntity(url, CustomerDto.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (HttpServerErrorException e) {
            throw new ExternalServiceException("Servicio de clientes no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("Servicio de clientes no disponible", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
