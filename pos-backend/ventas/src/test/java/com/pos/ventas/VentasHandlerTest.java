package com.pos.ventas;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para VentasHandler.
 * DynamoDB está completamente aislado mediante mocks de Mockito.
 * No se requiere infraestructura AWS real para ejecutar estas pruebas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VentasHandler - Pruebas Unitarias")
class VentasHandlerTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    private VentasHandler handler;

    private static final String TEST_PRODUCTOS_TABLE = "test-productos-table";
    private static final String TEST_VENTAS_TABLE    = "test-ventas-table";

    @BeforeEach
    void setUp() {
        // Inyectar el mock via constructor de test
        handler = new VentasHandler(dynamoDbClient, TEST_PRODUCTOS_TABLE, TEST_VENTAS_TABLE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Venta registrada exitosamente
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /ventas - Retorna HTTP 201 cuando la venta se registra correctamente")
    void testPostVentas_success() {
        // Arrange: producto existe en DynamoDB
        Map<String, AttributeValue> productoItem = Map.of(
                "id",     AttributeValue.fromS("1"),
                "nombre", AttributeValue.fromS("Laptop"),
                "precio", AttributeValue.fromN("2500.0")
        );
        GetItemResponse getItemResponse = GetItemResponse.builder()
                .item(productoItem)
                .build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(getItemResponse);
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody("{\"productos\": [{\"id\": \"1\", \"cantidad\": 2}]}");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        // Assert
        assertNotNull(response);
        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Venta registrada correctamente"));
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
        // Verificar que se llamó a putItem exactamente una vez
        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Body inválido (JSON malformado)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /ventas - Retorna HTTP 400 cuando el body es JSON inválido")
    void testPostVentas_invalidBody() {
        // Arrange: body no es JSON válido
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody("esto no es json valido {{{");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Body inválido"));
        // No debe llamar a DynamoDB
        verifyNoInteractions(dynamoDbClient);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Body con array de productos vacío
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /ventas - Retorna HTTP 400 cuando productos es un array vacío")
    void testPostVentas_emptyProductos() {
        // Arrange: productos es array vacío
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody("{\"productos\": []}");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Body inválido"));
        verifyNoInteractions(dynamoDbClient);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: Body nulo
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /ventas - Retorna HTTP 400 cuando el body es nulo")
    void testPostVentas_nullBody() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody(null);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Body inválido"));
        verifyNoInteractions(dynamoDbClient);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: Producto inexistente
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /ventas - Retorna HTTP 404 cuando el producto no existe en DynamoDB")
    void testPostVentas_productNotFound() {
        // Arrange: getItem retorna item vacío (producto no existe)
        GetItemResponse emptyResponse = GetItemResponse.builder()
                .item(Map.of())   // item vacío = no encontrado
                .build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(emptyResponse);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody("{\"productos\": [{\"id\": \"999\", \"cantidad\": 1}]}");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Producto inexistente"));
        // No debe llamar a putItem
        verify(dynamoDbClient, never()).putItem(any(PutItemRequest.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 6: Error de conexión con DynamoDB al escribir
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /ventas - Retorna HTTP 500 cuando DynamoDB falla al escribir la venta")
    void testPostVentas_dynamoWriteError() {
        // Arrange: getItem exitoso, putItem lanza excepción
        Map<String, AttributeValue> productoItem = Map.of(
                "id",     AttributeValue.fromS("1"),
                "nombre", AttributeValue.fromS("Laptop"),
                "precio", AttributeValue.fromN("2500.0")
        );
        GetItemResponse getItemResponse = GetItemResponse.builder()
                .item(productoItem)
                .build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(getItemResponse);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(DynamoDbException.builder()
                        .message("Write capacity exceeded")
                        .build());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody("{\"productos\": [{\"id\": \"1\", \"cantidad\": 1}]}");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        // Assert
        assertNotNull(response);
        assertEquals(500, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("message"));
        assertTrue(response.getBody().contains("Error de conexión con DynamoDB"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 7: Error de conexión con DynamoDB al verificar producto
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /ventas - Retorna HTTP 500 cuando DynamoDB falla al verificar el producto")
    void testPostVentas_dynamoReadError() {
        // Arrange: getItem lanza excepción
        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenThrow(DynamoDbException.builder()
                        .message("Connection timeout")
                        .build());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody("{\"productos\": [{\"id\": \"1\", \"cantidad\": 1}]}");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        // Assert
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Error de conexión con DynamoDB"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 8: Venta con múltiples productos
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /ventas - Registra correctamente una venta con múltiples productos")
    void testPostVentas_multipleProducts() {
        // Arrange: ambos productos existen
        Map<String, AttributeValue> productoItem = Map.of(
                "id",     AttributeValue.fromS("1"),
                "nombre", AttributeValue.fromS("Laptop"),
                "precio", AttributeValue.fromN("2500.0")
        );
        GetItemResponse getItemResponse = GetItemResponse.builder()
                .item(productoItem)
                .build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(getItemResponse);
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody("{\"productos\": [{\"id\": \"1\", \"cantidad\": 2}, {\"id\": \"2\", \"cantidad\": 1}]}");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        // Assert
        assertEquals(201, response.getStatusCode());
        assertTrue(response.getBody().contains("Venta registrada correctamente"));
        // getItem debe llamarse 2 veces (una por producto)
        verify(dynamoDbClient, times(2)).getItem(any(GetItemRequest.class));
        // putItem debe llamarse exactamente 1 vez
        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 9: Verificar que la venta guardada contiene id, productos y timestamp
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /ventas - El item guardado en DynamoDB contiene id, productos y timestamp")
    void testPostVentas_savedItemHasCorrectStructure() {
        // Arrange
        Map<String, AttributeValue> productoItem = Map.of(
                "id",     AttributeValue.fromS("1"),
                "nombre", AttributeValue.fromS("Laptop"),
                "precio", AttributeValue.fromN("2500.0")
        );
        GetItemResponse getItemResponse = GetItemResponse.builder().item(productoItem).build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(getItemResponse);

        ArgumentCaptor<PutItemRequest> putCaptor = ArgumentCaptor.forClass(PutItemRequest.class);
        when(dynamoDbClient.putItem(putCaptor.capture())).thenReturn(PutItemResponse.builder().build());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody("{\"productos\": [{\"id\": \"1\", \"cantidad\": 1}]}");

        // Act
        handler.handleRequest(request, null);

        // Assert: verificar estructura del item guardado
        PutItemRequest capturedRequest = putCaptor.getValue();
        Map<String, AttributeValue> savedItem = capturedRequest.item();
        assertTrue(savedItem.containsKey("id"),        "El item debe tener campo 'id'");
        assertTrue(savedItem.containsKey("productos"), "El item debe tener campo 'productos'");
        assertTrue(savedItem.containsKey("timestamp"), "El item debe tener campo 'timestamp'");
        assertNotNull(savedItem.get("id").s(),         "El id no debe ser nulo");
        assertNotNull(savedItem.get("timestamp").s(),  "El timestamp no debe ser nulo");
    }
}
