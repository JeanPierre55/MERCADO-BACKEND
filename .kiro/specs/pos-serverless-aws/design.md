# Design Document: POS Serverless AWS

## Overview

This document describes the technical design for a serverless Point of Sale (POS) backend built on AWS. The system exposes two HTTP endpoints — `GET /productos` and `POST /ventas` — implemented as independent Java 17 Lambda functions, backed by DynamoDB tables, and fronted by API Gateway. Infrastructure is defined entirely as code using AWS SAM (Serverless Application Model).

The project is scoped for academic evaluation and demonstrates serverless architecture patterns, infrastructure-as-code, Java Lambda development, and unit testing with mocked AWS dependencies.

### Design Goals

- **Simplicity**: Two focused Lambda functions, each with a single responsibility.
- **Testability**: Business logic is isolated from the AWS SDK so it can be unit-tested with Mockito without real AWS infrastructure.
- **Deployability**: A single `sam build && sam deploy` command provisions the entire stack.
- **Least privilege**: Each Lambda has only the IAM permissions it needs — nothing more.

---

## Architecture

### High-Level Flow

```
Client (Postman / Browser)
        │
        ▼
┌─────────────────────────────────────────────────────┐
│              AWS API Gateway (REST API)              │
│                                                     │
│   GET  /productos  ──────────────────────────────┐  │
│   POST /ventas     ──────────────────────────┐   │  │
└──────────────────────────────────────────────┼───┼──┘
                                               │   │
                    ┌──────────────────────────┘   │
                    │                              │
                    ▼                              ▼
        ┌───────────────────┐          ┌───────────────────┐
        │   VentasFunction  │          │ ProductosFunction │
        │   (Java 17)       │          │   (Java 17)       │
        │   VentasHandler   │          │ ProductosHandler  │
        └────────┬──────────┘          └────────┬──────────┘
                 │                              │
         ┌───────┴──────┐                       │
         │              │                       │
         ▼              ▼                       ▼
  ┌────────────┐ ┌────────────┐        ┌────────────────┐
  │  Ventas    │ │ Productos  │        │   Productos    │
  │  Table     │ │  Table     │        │    Table       │
  │ (DynamoDB) │ │ (DynamoDB) │        │  (DynamoDB)    │
  └────────────┘ └────────────┘        └────────────────┘
```

### Component Responsibilities

| Component | Responsibility |
|-----------|---------------|
| API Gateway | HTTP routing, request/response transformation, CORS headers |
| ProductosFunction | Scan ProductosTable, return product list as JSON |
| VentasFunction | Validate request, verify products exist, write sale to VentasTable |
| ProductosTable | Stores product catalog (id, nombre, precio) |
| VentasTable | Stores completed sale transactions |

### Request Lifecycle — GET /productos

1. Client sends `GET /productos`
2. API Gateway receives the request and invokes `ProductosFunction` via Lambda proxy integration
3. `ProductosHandler.handleRequest()` is called with an `APIGatewayProxyRequestEvent`
4. Handler calls `DynamoDbClient.scan()` on `ProductosTable`
5. Handler maps each `Map<String, AttributeValue>` to a `Producto` POJO
6. Handler serializes the list to JSON and returns `APIGatewayProxyResponseEvent` with status 200
7. API Gateway forwards the response to the client

### Request Lifecycle — POST /ventas

1. Client sends `POST /ventas` with JSON body
2. API Gateway invokes `VentasFunction` via Lambda proxy integration
3. `VentasHandler.handleRequest()` is called
4. Handler deserializes the request body; returns 400 if invalid
5. For each product in the request, handler calls `DynamoDbClient.getItem()` on `ProductosTable`; returns 404 if any product is missing
6. Handler generates a UUID as the sale id and captures the current ISO-8601 timestamp
7. Handler calls `DynamoDbClient.putItem()` on `VentasTable`
8. Handler returns 201 with success message

---

## Components and Interfaces

### ProductosHandler

**Package:** `com.pos.productos`  
**File:** `productos/src/main/java/com/pos/productos/ProductosHandler.java`

```java
public class ProductosHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    // Constructor used in production — reads env var, builds real DynamoDB client
    public ProductosHandler() { ... }

    // Constructor used in tests — accepts injected mock client
    public ProductosHandler(DynamoDbClient dynamoDbClient, String tableName) { ... }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) { ... }
}
```

**Key methods:**

| Method | Description |
|--------|-------------|
| `handleRequest(event, context)` | Entry point; orchestrates scan and response building |
| `scanProductos()` | Calls `DynamoDbClient.scan()`, maps results to `List<Producto>` |
| `buildResponse(int status, Object body)` | Serializes body to JSON, sets headers |
| `buildErrorResponse(int status, String message)` | Builds `{"message": "..."}` error body |

### VentasHandler

**Package:** `com.pos.ventas`  
**File:** `ventas/src/main/java/com/pos/ventas/VentasHandler.java`

```java
public class VentasHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient;
    private final String productosTableName;
    private final String ventasTableName;

    // Production constructor
    public VentasHandler() { ... }

    // Test constructor
    public VentasHandler(DynamoDbClient dynamoDbClient,
                         String productosTableName,
                         String ventasTableName) { ... }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) { ... }
}
```

**Key methods:**

| Method | Description |
|--------|-------------|
| `handleRequest(event, context)` | Entry point; orchestrates validation, verification, persistence |
| `parseRequestBody(String body)` | Deserializes JSON body to `VentaRequest`; throws on invalid JSON |
| `validateRequest(VentaRequest req)` | Checks `productos` field is non-null and non-empty |
| `verifyProductExists(String productId)` | Calls `DynamoDbClient.getItem()` on ProductosTable |
| `saveVenta(Venta venta)` | Calls `DynamoDbClient.putItem()` on VentasTable |
| `buildResponse(int status, Object body)` | Serializes body to JSON, sets headers |

### Request/Response DTOs

**VentaRequest** (deserialized from POST body):
```java
public class VentaRequest {
    private List<ProductoItem> productos;
    // getters/setters
}

public class ProductoItem {
    private String id;
    private int cantidad;
    // getters/setters
}
```

**Venta** (persisted to DynamoDB):
```java
public class Venta {
    private String id;           // UUID
    private List<ProductoItem> productos;
    private String timestamp;    // ISO-8601
    // getters/setters
}
```

**Producto** (read from DynamoDB):
```java
public class Producto {
    private String id;
    private String nombre;
    private Double precio;
    // getters/setters
}
```

### Shared Response Builder

Both handlers use the same pattern for building responses:

```java
private APIGatewayProxyResponseEvent buildResponse(int statusCode, String jsonBody) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Access-Control-Allow-Origin", "*");
    return new APIGatewayProxyResponseEvent()
            .withStatusCode(statusCode)
            .withHeaders(headers)
            .withBody(jsonBody);
}
```

---

## Data Models

### ProductosTable

| Attribute | DynamoDB Type | Description |
|-----------|--------------|-------------|
| `id` | String (S) | **Partition key** — unique product identifier |
| `nombre` | String (S) | Product display name |
| `precio` | Number (N) | Unit price |

**Access patterns:**
- `Scan` — full table scan to list all products (acceptable for small academic catalog)
- `GetItem` — point lookup by `id` to verify product existence during sale registration

**SAM definition:**
```yaml
ProductosTable:
  Type: AWS::DynamoDB::Table
  Properties:
    TableName: !Sub "${AWS::StackName}-productos"
    BillingMode: PAY_PER_REQUEST
    AttributeDefinitions:
      - AttributeName: id
        AttributeType: S
    KeySchema:
      - AttributeName: id
        KeyType: HASH
```

### VentasTable

| Attribute | DynamoDB Type | Description |
|-----------|--------------|-------------|
| `id` | String (S) | **Partition key** — UUID generated at registration time |
| `productos` | List (L) | Array of `{id, cantidad}` maps |
| `timestamp` | String (S) | ISO-8601 datetime of registration |

**Access patterns:**
- `PutItem` — write a new sale record

**SAM definition:**
```yaml
VentasTable:
  Type: AWS::DynamoDB::Table
  Properties:
    TableName: !Sub "${AWS::StackName}-ventas"
    BillingMode: PAY_PER_REQUEST
    AttributeDefinitions:
      - AttributeName: id
        AttributeType: S
    KeySchema:
      - AttributeName: id
        KeyType: HASH
```

### DynamoDB Attribute Mapping

**Producto → DynamoDB item:**
```
id      → AttributeValue.fromS(producto.getId())
nombre  → AttributeValue.fromS(producto.getNombre())
precio  → AttributeValue.fromN(producto.getPrecio().toString())
```

**DynamoDB item → Producto:**
```
item.get("id").s()      → producto.setId(...)
item.get("nombre").s()  → producto.setNombre(...)
item.get("precio").n()  → producto.setPrecio(Double.parseDouble(...))
```

**Venta → DynamoDB item:**
```
id        → AttributeValue.fromS(venta.getId())
timestamp → AttributeValue.fromS(venta.getTimestamp())
productos → AttributeValue.fromL(List<AttributeValue> of maps)
```

Each product item in the list is stored as:
```
{
  "id":       AttributeValue.fromS(item.getId()),
  "cantidad": AttributeValue.fromN(String.valueOf(item.getCantidad()))
}
```

---

## SAM Template Structure

The `template.yaml` at the project root defines all AWS resources:

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: POS Serverless AWS - Academic Project

Globals:
  Function:
    Runtime: java17
    Architectures: [x86_64]
    MemorySize: 512
    Timeout: 30
    Environment:
      Variables:
        PRODUCTOS_TABLE: !Ref ProductosTable

Resources:

  # ── API Gateway ──────────────────────────────────────────────────────────────
  PosApi:
    Type: AWS::Serverless::Api
    Properties:
      StageName: prod
      Cors:
        AllowMethods: "'GET,POST,OPTIONS'"
        AllowHeaders: "'Content-Type'"
        AllowOrigin: "'*'"

  # ── Lambda: ProductosFunction ─────────────────────────────────────────────────
  ProductosFunction:
    Type: AWS::Serverless::Function
    Properties:
      FunctionName: !Sub "${AWS::StackName}-productos"
      CodeUri: productos/
      Handler: com.pos.productos.ProductosHandler::handleRequest
      Runtime: java17
      Environment:
        Variables:
          PRODUCTOS_TABLE: !Ref ProductosTable
      Policies:
        - DynamoDBReadPolicy:
            TableName: !Ref ProductosTable
      Events:
        GetProductos:
          Type: Api
          Properties:
            RestApiId: !Ref PosApi
            Path: /productos
            Method: GET

  # ── Lambda: VentasFunction ────────────────────────────────────────────────────
  VentasFunction:
    Type: AWS::Serverless::Function
    Properties:
      FunctionName: !Sub "${AWS::StackName}-ventas"
      CodeUri: ventas/
      Handler: com.pos.ventas.VentasHandler::handleRequest
      Runtime: java17
      Environment:
        Variables:
          PRODUCTOS_TABLE: !Ref ProductosTable
          VENTAS_TABLE: !Ref VentasTable
      Policies:
        - DynamoDBReadPolicy:
            TableName: !Ref ProductosTable
        - DynamoDBWritePolicy:
            TableName: !Ref VentasTable
      Events:
        PostVentas:
          Type: Api
          Properties:
            RestApiId: !Ref PosApi
            Path: /ventas
            Method: POST

  # ── DynamoDB Tables ───────────────────────────────────────────────────────────
  ProductosTable:
    Type: AWS::DynamoDB::Table
    Properties:
      BillingMode: PAY_PER_REQUEST
      AttributeDefinitions:
        - AttributeName: id
          AttributeType: S
      KeySchema:
        - AttributeName: id
          KeyType: HASH

  VentasTable:
    Type: AWS::DynamoDB::Table
    Properties:
      BillingMode: PAY_PER_REQUEST
      AttributeDefinitions:
        - AttributeName: id
          AttributeType: S
      KeySchema:
        - AttributeName: id
          KeyType: HASH

Outputs:
  ApiUrl:
    Description: "API Gateway endpoint URL"
    Value: !Sub "https://${PosApi}.execute-api.${AWS::Region}.amazonaws.com/prod"
```

### IAM Policy Summary

| Lambda | Permission | Resource |
|--------|-----------|----------|
| ProductosFunction | `dynamodb:Scan`, `dynamodb:GetItem` | ProductosTable |
| VentasFunction | `dynamodb:GetItem` | ProductosTable |
| VentasFunction | `dynamodb:PutItem` | VentasTable |

SAM's built-in `DynamoDBReadPolicy` and `DynamoDBWritePolicy` macros expand to the exact permissions above, following least-privilege principles.

---

## API Contracts

### GET /productos

**Request:**
```
GET /productos HTTP/1.1
Host: {api-id}.execute-api.{region}.amazonaws.com
```
No request body or query parameters required.

**Response 200 — Success:**
```json
[
  {
    "id": "1",
    "nombre": "Laptop",
    "precio": 2500.0
  },
  {
    "id": "2",
    "nombre": "Mouse",
    "precio": 50.0
  }
]
```

**Response 200 — Empty table:**
```json
[]
```

**Response 500 — DynamoDB error:**
```json
{
  "message": "Error de conexión con DynamoDB"
}
```

---

### POST /ventas

**Request:**
```
POST /ventas HTTP/1.1
Host: {api-id}.execute-api.{region}.amazonaws.com
Content-Type: application/json

{
  "productos": [
    { "id": "1", "cantidad": 2 },
    { "id": "2", "cantidad": 1 }
  ]
}
```

**Field validation rules:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `productos` | Array | Yes | Non-null, non-empty |
| `productos[].id` | String | Yes | Non-null, non-empty |
| `productos[].cantidad` | Integer | Yes | >= 1 |

**Response 201 — Sale registered:**
```json
{
  "message": "Venta registrada correctamente"
}
```

**Response 400 — Invalid body:**
```json
{
  "message": "Body inválido"
}
```

**Response 404 — Product not found:**
```json
{
  "message": "Producto inexistente"
}
```

**Response 500 — DynamoDB error:**
```json
{
  "message": "Error de conexión con DynamoDB"
}
```

---

## Error Handling Strategy

### Error Decision Tree

```
Request received
    │
    ├─ Body is null or not valid JSON?
    │       └─ 400 {"message": "Body inválido"}
    │
    ├─ "productos" field missing or empty array?
    │       └─ 400 {"message": "Body inválido"}
    │
    ├─ Any product id not found in ProductosTable?
    │       └─ 404 {"message": "Producto inexistente"}
    │
    ├─ DynamoDB SDK throws exception?
    │       └─ 500 {"message": "Error de conexión con DynamoDB"}
    │
    └─ All checks pass
            └─ 201 {"message": "Venta registrada correctamente"}
```

### Exception Handling in Handlers

Both handlers wrap all DynamoDB calls in try-catch blocks:

```java
try {
    // DynamoDB operation
} catch (DynamoDbException e) {
    return buildErrorResponse(500, "Error de conexión con DynamoDB");
} catch (Exception e) {
    return buildErrorResponse(500, "Error interno del servidor");
}
```

For `VentasHandler`, JSON parsing uses a try-catch around `ObjectMapper.readValue()`:

```java
try {
    VentaRequest request = objectMapper.readValue(body, VentaRequest.class);
} catch (JsonProcessingException e) {
    return buildErrorResponse(400, "Body inválido");
}
```

### HTTP Status Code Reference

| Code | Scenario | Response Body |
|------|----------|---------------|
| 200 | Products retrieved successfully | JSON array of products |
| 201 | Sale registered successfully | `{"message": "Venta registrada correctamente"}` |
| 400 | Missing/invalid request body | `{"message": "Body inválido"}` |
| 404 | Product id not found in catalog | `{"message": "Producto inexistente"}` |
| 500 | DynamoDB connection or write failure | `{"message": "Error de conexión con DynamoDB"}` |

All responses include:
```
Content-Type: application/json
Access-Control-Allow-Origin: *
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

**Property Reflection:** After prework analysis, Properties 1 and 2 (originally "product list completeness" and "empty table returns empty array") were combined into a single property because the empty-table case is simply N=0 of the general case. All other properties are non-redundant and address distinct validation layers.

---

### Property 1: Product list size and structure

*For any* list of N products (including N=0) stored in ProductosTable, a GET /productos request SHALL return HTTP 200 with a JSON array of exactly N elements, and each element SHALL contain the `id` (string), `nombre` (string), and `precio` (number) fields.

**Validates: Requirements 1.3, 1.4, 1.5**

---

### Property 2: Producto serialization round-trip

*For any* valid `Producto` object with non-null `id`, `nombre`, and `precio`, serializing it to JSON and deserializing it back SHALL produce an object equal to the original.

**Validates: Requirements 5.2, 5.7**

---

### Property 3: Venta serialization round-trip

*For any* valid `Venta` object with non-null `id`, non-empty `productos` list, and non-null `timestamp`, serializing it to JSON and deserializing it back SHALL produce an object equal to the original.

**Validates: Requirements 5.5, 5.7**

---

### Property 4: Invalid body always returns 400

*For any* input that is not valid JSON, or any JSON object missing the `productos` field, or any JSON object where `productos` is an empty array, a POST /ventas request SHALL return HTTP 400 with body `{"message": "Body inválido"}`.

**Validates: Requirements 2.2, 2.3, 2.5**

---

### Property 5: Non-existent product always returns 404

*For any* POST /ventas request containing at least one product id that does not exist in ProductosTable, the handler SHALL return HTTP 404 with body `{"message": "Producto inexistente"}`, regardless of the other products in the request.

**Validates: Requirements 2.7, 11.3**

---

### Property 6: Successful sale is persisted with correct structure

*For any* valid POST /ventas request where all product ids exist in ProductosTable, the handler SHALL write exactly one item to VentasTable containing the `id` (UUID), `productos` array, and `timestamp` fields, and SHALL return HTTP 201 with body `{"message": "Venta registrada correctamente"}`.

**Validates: Requirements 2.8, 2.9, 2.10, 11.5**

---

### Property 7: All responses include required headers

*For any* request to either endpoint, regardless of success or error outcome, the response SHALL include `Content-Type: application/json` and `Access-Control-Allow-Origin: *` headers.

**Validates: Requirements 11.7, 11.8**

---

## Testing Strategy

### Overview

The testing strategy uses a dual approach: **unit tests** for business logic with mocked AWS dependencies, and **integration tests** (manual, post-deployment) for end-to-end validation. Property-based testing is applied to the pure logic and serialization layers.

### Unit Testing with Mockito

Each Lambda handler is designed for testability via constructor injection. The test constructor accepts a `DynamoDbClient` mock, allowing full isolation from AWS.

**ProductosHandlerTest scenarios:**

| Test | Mock Setup | Expected Result |
|------|-----------|-----------------|
| `testGetProductos_success` | `scan()` returns 2 items | HTTP 200, array with 2 products |
| `testGetProductos_emptyTable` | `scan()` returns empty list | HTTP 200, `[]` |
| `testGetProductos_dynamoError` | `scan()` throws `DynamoDbException` | HTTP 500, error message |

**VentasHandlerTest scenarios:**

| Test | Mock Setup | Expected Result |
|------|-----------|-----------------|
| `testPostVentas_success` | `getItem()` returns product, `putItem()` succeeds | HTTP 201, success message |
| `testPostVentas_invalidBody` | No mock needed | HTTP 400, "Body inválido" |
| `testPostVentas_emptyProductos` | No mock needed | HTTP 400, "Body inválido" |
| `testPostVentas_productNotFound` | `getItem()` returns empty item | HTTP 404, "Producto inexistente" |
| `testPostVentas_dynamoWriteError` | `putItem()` throws `DynamoDbException` | HTTP 500, error message |

**Mockito setup pattern:**
```java
@ExtendWith(MockitoExtension.class)
class ProductosHandlerTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    private ProductosHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductosHandler(dynamoDbClient, "test-productos-table");
    }

    @Test
    void testGetProductos_success() {
        // Arrange
        Map<String, AttributeValue> item = Map.of(
            "id",     AttributeValue.fromS("1"),
            "nombre", AttributeValue.fromS("Laptop"),
            "precio", AttributeValue.fromN("2500.0")
        );
        ScanResponse scanResponse = ScanResponse.builder().items(List.of(item)).build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);

        // Act
        APIGatewayProxyResponseEvent response =
            handler.handleRequest(new APIGatewayProxyRequestEvent(), null);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Laptop"));
    }
}
```

### Property-Based Testing

Property-based tests use **jqwik** (Java property-based testing library) to generate random inputs and verify universal properties.

**Library:** `net.jqwik:jqwik:1.8.x` (test scope)

**Configured iterations:** minimum 100 per property test.

**Tag format:** `@Tag("Feature: pos-serverless-aws, Property N: <property_text>")`

**Property tests to implement:**

| Property | Test Class | What Varies |
|----------|-----------|-------------|
| Property 1: Product list size and structure | `ProductosHandlerTest` | Random product lists (0 to N items) |
| Property 2: Producto serialization round-trip | `ProductoSerializationTest` | Random `id`, `nombre`, `precio` values |
| Property 3: Venta serialization round-trip | `VentaSerializationTest` | Random `id`, product lists, timestamps |
| Property 4: Invalid body → 400 | `VentasHandlerTest` | Random invalid JSON strings, missing fields, empty arrays |
| Property 5: Non-existent product → 404 | `VentasHandlerTest` | Random product ids not in mock table |
| Property 6: Successful sale persisted correctly | `VentasHandlerTest` | Random valid sale requests |
| Property 7: All responses include required headers | `ProductosHandlerTest`, `VentasHandlerTest` | All success and error scenarios |

**Example property test:**
```java
@Property(tries = 100)
@Tag("Feature: pos-serverless-aws, Property 3: Producto serialization round-trip")
void productoSerializationRoundTrip(
        @ForAll @AlphaChars @StringLength(min = 1, max = 20) String id,
        @ForAll @AlphaChars @StringLength(min = 1, max = 50) String nombre,
        @ForAll @DoubleRange(min = 0.01, max = 99999.99) double precio) throws Exception {

    Producto original = new Producto(id, nombre, precio);
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(original);
    Producto deserialized = mapper.readValue(json, Producto.class);

    assertEquals(original.getId(), deserialized.getId());
    assertEquals(original.getNombre(), deserialized.getNombre());
    assertEquals(original.getPrecio(), deserialized.getPrecio());
}
```

### Test Execution

```bash
# Run all unit and property tests
cd productos && mvn test
cd ventas && mvn test
```

### Integration Testing (Post-Deployment)

After `sam deploy`, validate end-to-end behavior using Postman or curl:

1. Insert sample data into ProductosTable via AWS CLI
2. `GET /productos` — verify 200 with product array
3. `POST /ventas` with valid body — verify 201
4. `POST /ventas` with missing `productos` — verify 400
5. `POST /ventas` with non-existent product id — verify 404

---

## Technical Justification

### Why Java 17?

Java 17 is the current LTS release and the most widely used runtime for enterprise Lambda functions. It provides:
- **Strong typing** — compile-time error detection, critical for academic evaluation
- **AWS SDK v2 native support** — the AWS SDK for Java v2 is designed for Java 8+ and works optimally on Java 17
- **Familiar tooling** — Maven, JUnit 5, Mockito are industry-standard tools taught in software engineering courses
- **Lambda support** — AWS Lambda officially supports Java 17 as a managed runtime

The tradeoff is cold start latency (typically 1–3 seconds for Java vs. ~100ms for Node.js). For an academic project with low traffic, this is acceptable. Production systems requiring sub-100ms cold starts would use SnapStart or a different runtime.

### Why AWS SAM?

AWS SAM (Serverless Application Model) is the official AWS framework for serverless IaC:
- **Single command deployment** — `sam build && sam deploy --guided` provisions the entire stack
- **Built-in Lambda support** — `AWS::Serverless::Function` is a higher-level abstraction over CloudFormation that reduces boilerplate
- **Local testing** — `sam local invoke` and `sam local start-api` allow local Lambda execution without deploying
- **Policy macros** — `DynamoDBReadPolicy` and `DynamoDBWritePolicy` generate least-privilege IAM policies automatically
- **Academic alignment** — SAM is the recommended starting point in AWS documentation for serverless beginners

### Why DynamoDB?

DynamoDB is the natural persistence layer for serverless applications on AWS:
- **No connection management** — unlike RDS, DynamoDB requires no VPC, no connection pool, no warm-up
- **On-demand billing** — `PAY_PER_REQUEST` mode means zero cost when idle, ideal for academic projects
- **AWS SDK v2 integration** — first-class support with `DynamoDbClient`
- **Simple schema** — the two-table design (products, sales) maps directly to DynamoDB's key-value model

### Why Constructor Injection for Testability?

Lambda functions cannot use Spring's dependency injection container (without additional frameworks like Micronaut or Quarkus). The pattern used here — a production no-arg constructor that builds real clients, plus a test constructor that accepts injected mocks — is the standard approach for testable Lambda functions:

```java
// Production: called by Lambda runtime
public ProductosHandler() {
    this.dynamoDbClient = DynamoDbClient.create();
    this.tableName = System.getenv("PRODUCTOS_TABLE");
}

// Test: called by unit tests with mocks
public ProductosHandler(DynamoDbClient dynamoDbClient, String tableName) {
    this.dynamoDbClient = dynamoDbClient;
    this.tableName = tableName;
}
```

This avoids reflection-based mocking of static factory methods and keeps tests fast and deterministic.
