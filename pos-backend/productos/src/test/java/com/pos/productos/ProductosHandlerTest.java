package com.pos.productos;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para ProductosHandler.
 * DynamoDB está completamente aislado mediante mocks de Mockito.
 * No se requiere infraestructura AWS real para ejecutar estas pruebas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductosHandler - Pruebas Unitarias")
class ProductosHandlerTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    private ProductosHandler handler;

    private static final String TEST_TABLE = "test-productos-table";

    @BeforeEach
    void setUp() {
        // Inyectar el mock via constructor de test
        handler = new ProductosHandler(dynamoDbClient, TEST_TABLE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Respuesta exitosa con productos
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /productos - Retorna HTTP 200 con lista de productos cuando la tabla tiene datos")
    void testGetProductos_success() {
        // Arrange: mock retorna 2 productos
        Map<String, AttributeValue> item1 = Map.of(
                "id",     AttributeValue.fromS("1"),
                "nombre", AttributeValue.fromS("Laptop"),
                "precio", AttributeValue.fromN("2500.0")
        );
        Map<String, AttributeValue> item2 = Map.of(
                "id",     AttributeValue.fromS("2"),
                "nombre", AttributeValue.fromS("Mouse"),
                "precio", AttributeValue.fromN("50.0")
        );
        ScanResponse scanResponse = ScanResponse.builder()
                .items(List.of(item1, item2))
                .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);

        // Act
        APIGatewayProxyResponseEvent response =
                handler.handleRequest(new APIGatewayProxyRequestEvent(), null);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Laptop"));
        assertTrue(response.getBody().contains("Mouse"));
        assertTrue(response.getBody().contains("2500.0"));
        assertTrue(response.getBody().contains("50.0"));
        // Verificar headers
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Tabla vacía
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /productos - Retorna HTTP 200 con array vacío cuando la tabla está vacía")
    void testGetProductos_emptyTable() {
        // Arrange: mock retorna lista vacía
        ScanResponse emptyScanResponse = ScanResponse.builder()
                .items(List.of())
                .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(emptyScanResponse);

        // Act
        APIGatewayProxyResponseEvent response =
                handler.handleRequest(new APIGatewayProxyRequestEvent(), null);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("[]", response.getBody());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Error de conexión con DynamoDB
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /productos - Retorna HTTP 500 cuando DynamoDB lanza excepción")
    void testGetProductos_dynamoError() {
        // Arrange: mock lanza DynamoDbException
        when(dynamoDbClient.scan(any(ScanRequest.class)))
                .thenThrow(DynamoDbException.builder()
                        .message("Connection refused")
                        .build());

        // Act
        APIGatewayProxyResponseEvent response =
                handler.handleRequest(new APIGatewayProxyRequestEvent(), null);

        // Assert
        assertNotNull(response);
        assertEquals(500, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("message"));
        assertTrue(response.getBody().contains("Error de conexión con DynamoDB"));
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests adicionales de estructura de respuesta
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /productos - La respuesta siempre incluye headers CORS")
    void testGetProductos_alwaysIncludesCorsHeaders() {
        // Arrange
        ScanResponse scanResponse = ScanResponse.builder().items(List.of()).build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);

        // Act
        APIGatewayProxyResponseEvent response =
                handler.handleRequest(new APIGatewayProxyRequestEvent(), null);

        // Assert
        assertNotNull(response.getHeaders());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("GET /productos - La respuesta de error también incluye headers CORS")
    void testGetProductos_errorResponseIncludesCorsHeaders() {
        // Arrange
        when(dynamoDbClient.scan(any(ScanRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Error").build());

        // Act
        APIGatewayProxyResponseEvent response =
                handler.handleRequest(new APIGatewayProxyRequestEvent(), null);

        // Assert
        assertNotNull(response.getHeaders());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
    }
}
