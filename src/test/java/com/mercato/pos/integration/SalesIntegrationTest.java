package com.mercato.pos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercato.pos.dto.AddItemRequest;
import com.mercato.pos.dto.CheckoutRequest;
import com.mercato.pos.dto.CreateSaleRequest;
import com.mercato.pos.dto.CustomerDto;
import com.mercato.pos.dto.PartialReturnRequest;
import com.mercato.pos.dto.ProductDto;
import com.mercato.pos.dto.ReturnItemRequest;
import com.mercato.pos.dto.ReturnRequest;
import com.mercato.pos.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestTemplate restTemplate;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Login para obtener token
        String loginRequest = objectMapper.writeValueAsString(
            new com.mercato.pos.dto.LoginRequest("cajero", "cajero123")
        );

        var response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginRequest))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        var loginResponse = objectMapper.readValue(responseBody, com.mercato.pos.dto.LoginResponse.class);
        authToken = loginResponse.token();
    }

    @Test
    void testCashFlowIntegration() throws Exception {
        // 1. Crear venta
        CreateSaleRequest createRequest = new CreateSaleRequest("TERM001", null);
        String createRequestJson = objectMapper.writeValueAsString(createRequest);

        var createResponse = mockMvc.perform(post("/api/sales")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(createRequestJson))
            .andExpect(status().isCreated())
            .andReturn();

        String saleResponseBody = createResponse.getResponse().getContentAsString();
        var saleResponse = objectMapper.readValue(saleResponseBody, com.mercato.pos.dto.SaleResponse.class);
        String saleId = saleResponse.id();

        // 2. Agregar item
        ProductDto product = new ProductDto("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10, "Category");
        when(restTemplate.getForEntity(anyString(), any())).thenReturn(
            new org.springframework.http.ResponseEntity<>(product, org.springframework.http.HttpStatus.OK)
        );

        AddItemRequest addItemRequest = new AddItemRequest("PROD001", "BAR001", 1);
        String addItemJson = objectMapper.writeValueAsString(addItemRequest);

        mockMvc.perform(post("/api/sales/{saleId}/items", saleId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(addItemJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)));

        // 3. Checkout CASH
        CheckoutRequest checkoutRequest = new CheckoutRequest("CASH", BigDecimal.valueOf(119.00));
        String checkoutJson = objectMapper.writeValueAsString(checkoutRequest);

        mockMvc.perform(post("/api/sales/{saleId}/checkout", saleId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(checkoutJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactionId", notNullValue()));
    }

    @Test
    void testCreditFlowIntegration() throws Exception {
        // 1. Crear venta con cliente
        CreateSaleRequest createRequest = new CreateSaleRequest("TERM001", "CUST001");
        String createRequestJson = objectMapper.writeValueAsString(createRequest);

        var createResponse = mockMvc.perform(post("/api/sales")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(createRequestJson))
            .andExpect(status().isCreated())
            .andReturn();

        String saleResponseBody = createResponse.getResponse().getContentAsString();
        var saleResponse = objectMapper.readValue(saleResponseBody, com.mercato.pos.dto.SaleResponse.class);
        String saleId = saleResponse.id();

        // 2. Agregar item
        ProductDto product = new ProductDto("PROD001", "Product 1", "BAR001", BigDecimal.valueOf(100), 10, "Category");
        CustomerDto customer = new CustomerDto("CUST001", "John Doe", "CC", "123456", "APPROVED");

        when(restTemplate.getForEntity(anyString(), any())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            if (url.contains("/products/")) {
                return new org.springframework.http.ResponseEntity<>(product, org.springframework.http.HttpStatus.OK);
            } else if (url.contains("/customers/")) {
                return new org.springframework.http.ResponseEntity<>(customer, org.springframework.http.HttpStatus.OK);
            }
            return null;
        });

        AddItemRequest addItemRequest = new AddItemRequest("PROD001", "BAR001", 1);
        String addItemJson = objectMapper.writeValueAsString(addItemRequest);

        mockMvc.perform(post("/api/sales/{saleId}/items", saleId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(addItemJson))
            .andExpect(status().isOk());

        // 3. Checkout CREDIT
        CheckoutRequest checkoutRequest = new CheckoutRequest("CREDIT", BigDecimal.ZERO);
        String checkoutJson = objectMapper.writeValueAsString(checkoutRequest);

        mockMvc.perform(post("/api/sales/{saleId}/checkout", saleId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(checkoutJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.creditReferenceNumber", notNullValue()));
    }

    @Test
    void testFreezeFlowIntegration() throws Exception {
        // 1. Crear venta
        CreateSaleRequest createRequest = new CreateSaleRequest("TERM001", null);
        String createRequestJson = objectMapper.writeValueAsString(createRequest);

        var createResponse = mockMvc.perform(post("/api/sales")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(createRequestJson))
            .andExpect(status().isCreated())
            .andReturn();

        String saleResponseBody = createResponse.getResponse().getContentAsString();
        var saleResponse = objectMapper.readValue(saleResponseBody, com.mercato.pos.dto.SaleResponse.class);
        String saleId = saleResponse.id();

        // 2. Congelar venta
        mockMvc.perform(post("/api/sales/{saleId}/freeze", saleId)
            .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", equalTo("FROZEN")));

        // 3. Reanudar venta
        mockMvc.perform(post("/api/sales/{saleId}/resume", saleId)
            .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", equalTo("ACTIVE")));
    }

    @Test
    void testCancelFlowIntegration() throws Exception {
        // 1. Crear venta
        CreateSaleRequest createRequest = new CreateSaleRequest("TERM001", null);
        String createRequestJson = objectMapper.writeValueAsString(createRequest);

        var createResponse = mockMvc.perform(post("/api/sales")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(createRequestJson))
            .andExpect(status().isCreated())
            .andReturn();

        String saleResponseBody = createResponse.getResponse().getContentAsString();
        var saleResponse = objectMapper.readValue(saleResponseBody, com.mercato.pos.dto.SaleResponse.class);
        String saleId = saleResponse.id();

        // 2. Cancelar venta
        com.mercato.pos.dto.CancelSaleRequest cancelRequest = new com.mercato.pos.dto.CancelSaleRequest("Changed mind");
        String cancelJson = objectMapper.writeValueAsString(cancelRequest);

        mockMvc.perform(post("/api/sales/{saleId}/cancel", saleId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cancelJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", equalTo("CANCELLED")));
    }
}
