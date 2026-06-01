# Plan de Implementación: POS Serverless AWS

## Descripción General

Implementación de un backend serverless en AWS con dos endpoints HTTP (`GET /productos` y `POST /ventas`), dos funciones Lambda en Java 17, dos tablas DynamoDB, e infraestructura definida como código con AWS SAM. El proyecto sigue la metodología Spec-Driven Development (SDD).

## Tareas

- [x] 1. Crear especificaciones SDD (completado)
  - Requirements.md, design.md y tasks.md generados antes de cualquier implementación
  - _Requisitos: 9.11, 9.12_

- [x] 2. Crear estructura del proyecto
  - [x] 2.1 Crear directorios para el módulo `productos`
    - Crear árbol: `productos/src/main/java/com/pos/productos/model/`
    - Crear árbol: `productos/src/test/java/com/pos/productos/`
    - _Requisitos: 8.4, 8.5, 8.6_
  - [x] 2.2 Crear directorios para el módulo `ventas`
    - Crear árbol: `ventas/src/main/java/com/pos/ventas/model/`
    - Crear árbol: `ventas/src/test/java/com/pos/ventas/`
    - _Requisitos: 8.10, 8.11, 8.12_

- [x] 3. Configurar `pom.xml` para cada módulo Lambda
  - [x] 3.1 Crear `productos/pom.xml`
    - Declarar Java 17 como `source` y `target` en `maven-compiler-plugin`
    - Agregar dependencias: `aws-lambda-java-core`, `aws-lambda-java-events`, `software.amazon.awssdk:dynamodb`
    - Agregar dependencias de test: `junit-jupiter`, `mockito-core`, `mockito-junit-jupiter`, `net.jqwik:jqwik:1.8.x`
    - Configurar `maven-shade-plugin` para generar uber JAR con `finalName`
    - _Requisitos: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8_
  - [x] 3.2 Crear `ventas/pom.xml`
    - Mismas dependencias que `productos/pom.xml` más `jackson-databind` para deserialización JSON
    - Configurar `maven-shade-plugin` para generar uber JAR
    - _Requisitos: 7.9, 7.10, 7.11, 7.12_

- [x] 4. Crear modelos Java
  - [x] 4.1 Crear `Producto.java` en `productos/src/main/java/com/pos/productos/model/`
    - Campos: `id` (String), `nombre` (String), `precio` (Double)
    - Getters, setters y constructor sin argumentos para Jackson
    - _Requisitos: 5.1, 5.2, 5.3, 5.7_
  - [x] 4.2 Crear `ProductoItem.java` en `ventas/src/main/java/com/pos/ventas/model/`
    - Campos: `id` (String), `cantidad` (int)
    - Getters, setters y constructor sin argumentos
    - _Requisitos: 2.4, 5.7_
  - [x] 4.3 Crear `VentaRequest.java` en `ventas/src/main/java/com/pos/ventas/model/`
    - Campo: `productos` (List\<ProductoItem\>)
    - Getters, setters y constructor sin argumentos para Jackson
    - _Requisitos: 2.3, 2.4_
  - [x] 4.4 Crear `Venta.java` en `ventas/src/main/java/com/pos/ventas/model/`
    - Campos: `id` (String), `productos` (List\<ProductoItem\>), `timestamp` (String)
    - Getters, setters y constructor sin argumentos
    - _Requisitos: 5.4, 5.5, 5.6, 5.7_

- [x] 5. Implementar `ProductosHandler.java`
  - [x] 5.1 Crear `ProductosHandler.java` en `productos/src/main/java/com/pos/productos/`
    - Implementar `RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`
    - Constructor de producción: inicializar `DynamoDbClient.create()` y leer `PRODUCTOS_TABLE` de env var
    - Constructor de test: aceptar `DynamoDbClient` y `tableName` inyectados
    - Método `scanProductos()`: llamar `DynamoDbClient.scan()` y mapear items a `List<Producto>`
    - Método `buildResponse(int, Object)`: serializar a JSON, incluir headers `Content-Type` y `Access-Control-Allow-Origin: *`
    - Método `buildErrorResponse(int, String)`: construir `{"message": "..."}` con headers correctos
    - Envolver llamadas DynamoDB en try-catch para `DynamoDbException` → HTTP 500
    - _Requisitos: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 4.1, 4.2, 4.3, 4.4, 4.9, 4.11, 4.12, 11.7, 11.8_

- [-] 6. Implementar `VentasHandler.java`
  - [ ] 6.1 Crear `VentasHandler.java` en `ventas/src/main/java/com/pos/ventas/`
    - Implementar `RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`
    - Constructor de producción: inicializar `DynamoDbClient.create()`, leer `PRODUCTOS_TABLE` y `VENTAS_TABLE` de env vars
    - Constructor de test: aceptar `DynamoDbClient`, `productosTableName` y `ventasTableName` inyectados
    - Método `parseRequestBody(String)`: deserializar JSON a `VentaRequest` con Jackson; lanzar excepción si inválido → HTTP 400
    - Método `validateRequest(VentaRequest)`: verificar que `productos` no sea null ni vacío → HTTP 400
    - Método `verifyProductExists(String)`: llamar `DynamoDbClient.getItem()` en ProductosTable; retornar false si item vacío → HTTP 404
    - Método `saveVenta(Venta)`: llamar `DynamoDbClient.putItem()` en VentasTable con id (UUID), productos y timestamp ISO-8601
    - Método `buildResponse` y `buildErrorResponse` con misma firma que ProductosHandler
    - Envolver llamadas DynamoDB en try-catch para `DynamoDbException` → HTTP 500
    - _Requisitos: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 2.11, 4.5, 4.6, 4.7, 4.8, 4.10, 4.11, 4.12, 11.1, 11.2, 11.3, 11.4, 11.5, 11.7, 11.8_

- [x] 7. Crear `template.yaml` AWS SAM
  - [x] 7.1 Crear `template.yaml` en la raíz del proyecto
    - Definir `AWSTemplateFormatVersion: '2010-09-09'` y `Transform: AWS::Serverless-2016-10-31`
    - Definir `Globals` con runtime `java17`, arquitectura `x86_64`, `MemorySize: 512`, `Timeout: 30`
    - Definir recurso `PosApi` (`AWS::Serverless::Api`) con `StageName: prod` y CORS habilitado
    - Definir `ProductosFunction` con `CodeUri: productos/`, handler `com.pos.productos.ProductosHandler::handleRequest`, evento GET `/productos`, política `DynamoDBReadPolicy` sobre `ProductosTable`, env var `PRODUCTOS_TABLE`
    - Definir `VentasFunction` con `CodeUri: ventas/`, handler `com.pos.ventas.VentasHandler::handleRequest`, evento POST `/ventas`, políticas `DynamoDBReadPolicy` sobre `ProductosTable` y `DynamoDBWritePolicy` sobre `VentasTable`, env vars `PRODUCTOS_TABLE` y `VENTAS_TABLE`
    - Definir `ProductosTable` (`AWS::DynamoDB::Table`) con `BillingMode: PAY_PER_REQUEST`, clave primaria `id` (String)
    - Definir `VentasTable` (`AWS::DynamoDB::Table`) con `BillingMode: PAY_PER_REQUEST`, clave primaria `id` (String)
    - Agregar sección `Outputs` con la URL del API Gateway
    - _Requisitos: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 3.11, 3.12, 3.13, 3.14, 3.15, 3.16, 3.17, 3.18, 10.7, 10.8, 10.9_

- [-] 8. Escribir pruebas unitarias `ProductosHandlerTest.java`
  - [x] 8.1 Crear `ProductosHandlerTest.java` en `productos/src/test/java/com/pos/productos/`
    - Anotar con `@ExtendWith(MockitoExtension.class)`
    - Declarar `@Mock DynamoDbClient dynamoDbClient` e inicializar handler en `@BeforeEach`
    - _Requisitos: 6.1, 6.2, 6.3_
  - [ ]* 8.2 Escribir test `testGetProductos_success`
    - Configurar mock: `scan()` retorna `ScanResponse` con 2 items (`id`, `nombre`, `precio`)
    - Verificar: HTTP 200, body contiene los nombres de los productos
    - _Requisitos: 6.4, 6.14, 6.15_
  - [ ]* 8.3 Escribir test `testGetProductos_emptyTable`
    - Configurar mock: `scan()` retorna `ScanResponse` con lista vacía
    - Verificar: HTTP 200, body es `[]`
    - _Requisitos: 6.5, 6.14, 6.15_
  - [ ]* 8.4 Escribir test `testGetProductos_dynamoError`
    - Configurar mock: `scan()` lanza `DynamoDbException`
    - Verificar: HTTP 500, body contiene campo `message`
    - _Requisitos: 6.6, 6.14, 6.15_

- [-] 9. Escribir pruebas unitarias `VentasHandlerTest.java`
  - [x] 9.1 Crear `VentasHandlerTest.java` en `ventas/src/test/java/com/pos/ventas/`
    - Anotar con `@ExtendWith(MockitoExtension.class)`
    - Declarar `@Mock DynamoDbClient dynamoDbClient` e inicializar handler en `@BeforeEach`
    - _Requisitos: 6.7, 6.8, 6.9_
  - [ ]* 9.2 Escribir test `testPostVentas_success`
    - Configurar mock: `getItem()` retorna item con producto existente; `putItem()` retorna respuesta exitosa
    - Verificar: HTTP 201, body `{"message": "Venta registrada correctamente"}`
    - _Requisitos: 6.10, 6.14, 6.15_
  - [ ]* 9.3 Escribir test `testPostVentas_invalidBody`
    - Enviar body con JSON inválido (ej. texto plano)
    - Verificar: HTTP 400, body `{"message": "Body inválido"}`
    - _Requisitos: 6.11, 6.14, 6.15_
  - [ ]* 9.4 Escribir test `testPostVentas_emptyProductos`
    - Enviar body con `{"productos": []}`
    - Verificar: HTTP 400, body `{"message": "Body inválido"}`
    - _Requisitos: 6.11, 6.14, 6.15_
  - [ ]* 9.5 Escribir test `testPostVentas_productNotFound`
    - Configurar mock: `getItem()` retorna `GetItemResponse` con item vacío (sin atributos)
    - Verificar: HTTP 404, body `{"message": "Producto inexistente"}`
    - _Requisitos: 6.12, 6.14, 6.15_
  - [ ]* 9.6 Escribir test `testPostVentas_dynamoWriteError`
    - Configurar mock: `getItem()` retorna producto existente; `putItem()` lanza `DynamoDbException`
    - Verificar: HTTP 500, body contiene campo `message`
    - _Requisitos: 6.13, 6.14, 6.15_

- [ ] 10. Escribir property tests con jqwik
  - [ ]* 10.1 Escribir Property 1: tamaño y estructura de lista de productos
    - Crear clase `ProductosHandlerPropertyTest` en `productos/src/test/java/com/pos/productos/`
    - Anotar con `@Tag("Feature: pos-serverless-aws, Property 1: Product list size and structure")`
    - Generar listas aleatorias de 0 a N productos con `@ForAll @Size(max=20) List<...>`
    - Configurar mock para retornar exactamente N items; verificar HTTP 200 y array de exactamente N elementos con campos `id`, `nombre`, `precio`
    - Usar `@Property(tries = 100)`
    - _Requisitos: 1.3, 1.4, 1.5_
  - [ ]* 10.2 Escribir Property 2: round-trip de serialización de `Producto`
    - Crear clase `ProductoSerializationTest` en `productos/src/test/java/com/pos/productos/`
    - Anotar con `@Tag("Feature: pos-serverless-aws, Property 2: Producto serialization round-trip")`
    - Generar `id`, `nombre` con `@AlphaChars @StringLength(min=1, max=20)` y `precio` con `@DoubleRange(min=0.01, max=99999.99)`
    - Serializar `Producto` a JSON con Jackson y deserializar de vuelta; verificar igualdad campo a campo
    - _Requisitos: 5.2, 5.7_
  - [ ]* 10.3 Escribir Property 3: round-trip de serialización de `Venta`
    - Crear clase `VentaSerializationTest` en `ventas/src/test/java/com/pos/ventas/`
    - Anotar con `@Tag("Feature: pos-serverless-aws, Property 3: Venta serialization round-trip")`
    - Generar `id`, lista de `ProductoItem` y `timestamp` aleatorios
    - Serializar `Venta` a JSON y deserializar de vuelta; verificar igualdad campo a campo
    - _Requisitos: 5.5, 5.7_
  - [ ]* 10.4 Escribir Property 4: body inválido siempre retorna 400
    - Agregar método de propiedad en `VentasHandlerPropertyTest` en `ventas/src/test/java/com/pos/ventas/`
    - Anotar con `@Tag("Feature: pos-serverless-aws, Property 4: Invalid body always returns 400")`
    - Generar strings arbitrarios que no sean JSON válido, objetos JSON sin campo `productos`, y objetos con `productos: []`
    - Verificar: HTTP 400 y body `{"message": "Body inválido"}` en todos los casos
    - _Requisitos: 2.2, 2.3, 2.5_
  - [ ]* 10.5 Escribir Property 5: producto inexistente siempre retorna 404
    - Agregar método de propiedad en `VentasHandlerPropertyTest`
    - Anotar con `@Tag("Feature: pos-serverless-aws, Property 5: Non-existent product always returns 404")`
    - Generar requests válidos con al menos un `id` de producto que no exista en el mock (mock retorna item vacío)
    - Verificar: HTTP 404 y body `{"message": "Producto inexistente"}` independientemente de los otros productos
    - _Requisitos: 2.7, 11.3_
  - [ ]* 10.6 Escribir Property 6: venta exitosa persiste con estructura correcta
    - Agregar método de propiedad en `VentasHandlerPropertyTest`
    - Anotar con `@Tag("Feature: pos-serverless-aws, Property 6: Successful sale is persisted with correct structure")`
    - Generar requests válidos donde todos los productos existen en el mock; capturar el argumento de `putItem()` con `ArgumentCaptor`
    - Verificar: HTTP 201, body `{"message": "Venta registrada correctamente"}`, y que el item guardado contiene `id` (UUID), `productos` y `timestamp`
    - _Requisitos: 2.8, 2.9, 2.10, 11.5_
  - [ ]* 10.7 Escribir Property 7: todas las respuestas incluyen headers requeridos
    - Agregar métodos de propiedad en `ProductosHandlerPropertyTest` y `VentasHandlerPropertyTest`
    - Anotar con `@Tag("Feature: pos-serverless-aws, Property 7: All responses include required headers")`
    - Para todos los escenarios (éxito y error) de ambos handlers, verificar que la respuesta incluye `Content-Type: application/json` y `Access-Control-Allow-Origin: *`
    - _Requisitos: 11.7, 11.8_

- [-] 11. Ejecutar pruebas unitarias y de propiedades
  - [ ] 11.1 Ejecutar `mvn test` en el módulo `productos`
    - Verificar que los 3 tests unitarios y los property tests de `ProductosHandlerTest` pasan
    - Corregir cualquier error de compilación o fallo de test antes de continuar
    - _Requisitos: 7.13_
  - [ ] 11.2 Ejecutar `mvn test` en el módulo `ventas`
    - Verificar que los 4 tests unitarios y los property tests de `VentasHandlerTest` pasan
    - Corregir cualquier error de compilación o fallo de test antes de continuar
    - _Requisitos: 7.13_

- [x] 12. Crear `.gitignore`
  - Crear `.gitignore` en la raíz del proyecto
  - Excluir: `target/`, `.aws-sam/`, `*.iml`, `.idea/`, `.vscode/`, `.env`, `~/.aws/credentials`, `samconfig.toml`
  - _Requisitos: 8.3, 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 13. Ejecutar `sam build`
  - Ejecutar `sam build` desde la raíz del proyecto
  - Verificar que ambos módulos compilan y el directorio `.aws-sam/build/` se genera correctamente
  - Corregir errores de build antes de continuar
  - _Requisitos: 3.1, 7.14_

- [ ] 14. Ejecutar `sam deploy --guided`
  - Ejecutar `sam deploy --guided` para desplegar el stack en AWS
  - Ingresar nombre del stack (ej. `pos-serverless-aws`), región, y confirmar creación de roles IAM
  - Anotar la URL del API Gateway del output `ApiUrl`
  - _Requisitos: 3.1, 3.14, 3.15, 3.16_

- [ ] 15. Insertar datos de prueba en `ProductosTable`
  - Insertar producto 1 vía AWS CLI o consola: `id="1"`, `nombre="Laptop"`, `precio=2500`
  - Insertar producto 2 vía AWS CLI o consola: `id="2"`, `nombre="Mouse"`, `precio=50`
  - Verificar que los items aparecen en la tabla con `aws dynamodb scan`
  - _Requisitos: 12.1, 12.2, 12.3, 12.4, 12.5_

- [ ] 16. Probar endpoints con Postman
  - Crear colección Postman con la URL base del API Gateway desplegado
  - Probar `GET /productos`: verificar HTTP 200 y array con los 2 productos insertados
  - Probar `POST /ventas` con body válido `{"productos": [{"id": "1", "cantidad": 2}]}`: verificar HTTP 201
  - Probar `POST /ventas` con body inválido: verificar HTTP 400
  - Probar `POST /ventas` con producto inexistente `{"productos": [{"id": "999", "cantidad": 1}]}`: verificar HTTP 404
  - _Requisitos: 9.7, 9.8, 9.9_

- [-] 17. Documentar `README.md` completo
  - [x] 17.1 Crear `README.md` en la raíz del proyecto
    - Sección: descripción del proyecto y objetivo académico
    - Sección: diagrama o descripción de la arquitectura serverless (API Gateway → Lambda → DynamoDB)
    - Sección: estructura del repositorio con todos los directorios y archivos clave
    - Sección: prerrequisitos (Java 17, Maven, AWS CLI, AWS SAM CLI, cuenta AWS configurada)
    - Sección: instrucciones de build (`sam build`), deploy (`sam deploy --guided`) y pruebas (`mvn test`)
    - Sección: cómo probar los endpoints con Postman (ejemplos de request/response)
    - Sección: comandos AWS CLI para insertar datos de prueba en ProductosTable
    - Sección: "Proceso SDD" explicando que specs fueron escritas antes de la implementación
    - _Requisitos: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9, 9.10, 9.11, 9.12_

- [ ] 18. Preparar entrega académica
  - [x] 18.1 Crear `my-prompt.txt` en la raíz del proyecto
    - Documentar el prompt principal utilizado para generar el spec con SDD
    - Incluir el prompt de generación de requirements, design y tasks
  - [ ] 18.2 Escribir reflexión sobre el proceso SDD
    - Describir cómo el enfoque SDD (specs primero, código después) influyó en el diseño
    - Incluir lecciones aprendidas sobre arquitectura serverless y testing con mocks
    - Agregar la reflexión al README.md o como archivo separado `reflexion.md`
    - _Requisitos: 9.11, 9.12_

## Notas

- Las tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido
- Cada tarea referencia requisitos específicos para trazabilidad completa
- Los checkpoints de pruebas (tareas 11.1 y 11.2) deben completarse antes de proceder al despliegue
- Los property tests usan jqwik con mínimo 100 iteraciones por propiedad (`@Property(tries = 100)`)
- Las pruebas unitarias usan JUnit 5 + Mockito con inyección de dependencias por constructor
- El patrón de dos constructores (producción + test) es obligatorio en ambos handlers para testabilidad sin infraestructura AWS real
