package com.pos.ventas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.ventas.model.ProductoItem;
import com.pos.ventas.model.Venta;
import com.pos.ventas.model.VentaRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AWS Lambda handler para el endpoint POST /ventas.
 * Valida el request, verifica que los productos existan en DynamoDB,
 * y registra la venta en la tabla VentasTable.
 *
 * Patrón de doble constructor para testabilidad:
 * - Constructor sin argumentos: usado por el runtime de Lambda (producción)
 * - Constructor con parámetros: usado en tests unitarios con mocks inyectados
 */
public class VentasHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient;
    private final String productosTableName;
    private final String ventasTableName;
    private final ObjectMapper objectMapper;

    /**
     * Constructor de producción — invocado por el runtime de AWS Lambda.
     * Lee los nombres de las tablas desde variables de entorno.
     */
    public VentasHandler() {
        this.dynamoDbClient = DynamoDbClient.create();
        this.productosTableName = System.getenv("PRODUCTOS_TABLE");
        this.ventasTableName = System.getenv("VENTAS_TABLE");
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Constructor de test — permite inyectar un DynamoDbClient mock.
     * Usado exclusivamente en pruebas unitarias.
     */
    public VentasHandler(DynamoDbClient dynamoDbClient,
                         String productosTableName,
                         String ventasTableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.productosTableName = productosTableName;
        this.ventasTableName = ventasTableName;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        // 1. Validar que el body no sea nulo
        String body = input.getBody();
        if (body == null || body.trim().isEmpty()) {
            return buildErrorResponse(400, "Body inválido");
        }

        // 2. Parsear el body JSON
        VentaRequest ventaRequest;
        try {
            ventaRequest = parseRequestBody(body);
        } catch (JsonProcessingException e) {
            return buildErrorResponse(400, "Body inválido");
        }

        // 3. Validar que el request tenga productos
        if (!validateRequest(ventaRequest)) {
            return buildErrorResponse(400, "Body inválido");
        }

        // 4. Verificar que cada producto exista en ProductosTable
        try {
            for (ProductoItem item : ventaRequest.getProductos()) {
                if (!verifyProductExists(item.getId())) {
                    return buildErrorResponse(404, "Producto inexistente");
                }
            }
        } catch (DynamoDbException e) {
            return buildErrorResponse(500, "Error de conexión con DynamoDB");
        }

        // 5. Guardar la venta en VentasTable
        try {
            Venta venta = new Venta(
                    UUID.randomUUID().toString(),
                    ventaRequest.getProductos(),
                    Instant.now().toString()
            );
            saveVenta(venta);
        } catch (DynamoDbException e) {
            return buildErrorResponse(500, "Error de conexión con DynamoDB");
        }

        // 6. Retornar respuesta exitosa
        return buildResponse(201, "{\"message\": \"Venta registrada correctamente\"}");
    }

    /**
     * Deserializa el body JSON a un objeto VentaRequest.
     * Lanza JsonProcessingException si el JSON es inválido.
     */
    VentaRequest parseRequestBody(String body) throws JsonProcessingException {
        return objectMapper.readValue(body, VentaRequest.class);
    }

    /**
     * Valida que el VentaRequest tenga la lista de productos no nula y no vacía.
     */
    boolean validateRequest(VentaRequest request) {
        return request != null
                && request.getProductos() != null
                && !request.getProductos().isEmpty();
    }

    /**
     * Verifica si un producto con el ID dado existe en ProductosTable.
     * Retorna true si existe, false si no existe.
     * Lanza DynamoDbException si hay error de conexión.
     */
    boolean verifyProductExists(String productId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.fromS(productId));

        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(productosTableName)
                .key(key)
                .build();

        GetItemResponse response = dynamoDbClient.getItem(getItemRequest);
        return response.hasItem() && !response.item().isEmpty();
    }

    /**
     * Persiste una Venta en la tabla VentasTable de DynamoDB.
     * Convierte la lista de productos a una lista de AttributeValue maps.
     */
    void saveVenta(Venta venta) {
        List<AttributeValue> productosAttr = new ArrayList<>();
        for (ProductoItem item : venta.getProductos()) {
            Map<String, AttributeValue> itemMap = new HashMap<>();
            itemMap.put("id", AttributeValue.fromS(item.getId()));
            itemMap.put("cantidad", AttributeValue.fromN(String.valueOf(item.getCantidad())));
            productosAttr.add(AttributeValue.fromM(itemMap));
        }

        Map<String, AttributeValue> itemToPut = new HashMap<>();
        itemToPut.put("id", AttributeValue.fromS(venta.getId()));
        itemToPut.put("timestamp", AttributeValue.fromS(venta.getTimestamp()));
        itemToPut.put("productos", AttributeValue.fromL(productosAttr));

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(ventasTableName)
                .item(itemToPut)
                .build();

        dynamoDbClient.putItem(putItemRequest);
    }

    /**
     * Construye una respuesta HTTP con el body JSON proporcionado.
     * Incluye headers Content-Type y CORS.
     */
    APIGatewayProxyResponseEvent buildResponse(int statusCode, String jsonBody) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type");

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(jsonBody);
    }

    /**
     * Construye una respuesta HTTP de error con el mensaje proporcionado.
     * El body tiene el formato: {"message": "<mensaje>"}
     */
    APIGatewayProxyResponseEvent buildErrorResponse(int statusCode, String message) {
        String errorBody = "{\"message\": \"" + message + "\"}";
        return buildResponse(statusCode, errorBody);
    }
}
