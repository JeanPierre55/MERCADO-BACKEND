package com.pos.productos;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.productos.model.Producto;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS Lambda handler para el endpoint GET /productos.
 * Consulta la tabla DynamoDB ProductosTable y retorna todos los productos.
 *
 * Patrón de doble constructor para testabilidad:
 * - Constructor sin argumentos: usado por el runtime de Lambda (producción)
 * - Constructor con parámetros: usado en tests unitarios con mocks inyectados
 */
public class ProductosHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final ObjectMapper objectMapper;

    /**
     * Constructor de producción — invocado por el runtime de AWS Lambda.
     * Lee el nombre de la tabla desde la variable de entorno PRODUCTOS_TABLE.
     */
    public ProductosHandler() {
        this.dynamoDbClient = DynamoDbClient.create();
        this.tableName = System.getenv("PRODUCTOS_TABLE");
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Constructor de test — permite inyectar un DynamoDbClient mock.
     * Usado exclusivamente en pruebas unitarias.
     */
    public ProductosHandler(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        try {
            List<Producto> productos = scanProductos();
            String jsonBody = objectMapper.writeValueAsString(productos);
            return buildResponse(200, jsonBody);
        } catch (DynamoDbException e) {
            return buildErrorResponse(500, "Error de conexión con DynamoDB");
        } catch (Exception e) {
            return buildErrorResponse(500, "Error interno del servidor");
        }
    }

    /**
     * Realiza un Scan completo de la tabla ProductosTable y mapea los resultados
     * a una lista de objetos Producto.
     */
    List<Producto> scanProductos() {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .build();

        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

        List<Producto> productos = new ArrayList<>();
        for (Map<String, AttributeValue> item : scanResponse.items()) {
            Producto producto = mapToProducto(item);
            productos.add(producto);
        }
        return productos;
    }

    /**
     * Convierte un item de DynamoDB (Map de AttributeValue) a un objeto Producto.
     */
    private Producto mapToProducto(Map<String, AttributeValue> item) {
        Producto producto = new Producto();
        if (item.containsKey("id")) {
            producto.setId(item.get("id").s());
        }
        if (item.containsKey("nombre")) {
            producto.setNombre(item.get("nombre").s());
        }
        if (item.containsKey("precio")) {
            producto.setPrecio(Double.parseDouble(item.get("precio").n()));
        }
        return producto;
    }

    /**
     * Construye una respuesta HTTP exitosa con el body JSON proporcionado.
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
