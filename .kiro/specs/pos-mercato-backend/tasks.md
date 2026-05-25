# Plan de Implementación: POS MERCATO Backend

## Overview

Implementación incremental de la API REST en Java 17 + Spring Boot 3.x para el sistema POS MERCATO. Cada tarea construye sobre la anterior, comenzando por la estructura del proyecto y terminando con los flujos de integración completos. Los cálculos monetarios se realizan en centavos (long) y se exponen como BigDecimal con 2 decimales.

## Tasks

- [x] 1. Inicializar proyecto Spring Boot y estructura base
  - Crear proyecto Maven con Spring Boot 3.x, Java 17
  - Agregar dependencias: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-validation, h2, jjwt, springdoc-openapi-starter-webmvc-ui, jqwik, wiremock-jre8
  - Crear estructura de paquetes: `com.mercato.pos.{config,controller,service,repository,model,dto,exception,scheduler,util}`
  - Crear `application.yml` con configuración de puerto 8080, H2 en memoria, JWT secret, URLs de APIs externas, tax-rate 0.19, frozen-sale-expiration-hours 2
  - Configurar JaCoCo en `pom.xml` con reglas de cobertura mínima (90% servicios, 80% proyecto)
  - _Requirements: 2.1, 15.1, 17.1, 18.5, 19.8_

- [-] 2. Implementar utilidad monetaria y entidades JPA
  - [ ] 2.1 Implementar `MoneyCalculator` con métodos `toCents(BigDecimal)`, `fromCents(long)` y `recalculateTotals(Sale)`
    - Usar aritmética entera en centavos para todos los cálculos intermedios
    - Retornar BigDecimal con escala 2 en `fromCents`
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_

  - [ ]* 2.2 Escribir property test para `MoneyCalculator` — Property 4: Round-trip centavos ↔ BigDecimal
    - **Property 4: `toCents(fromCents(x)) == x` para cualquier long ≥ 0**
    - **Validates: Requirements 7.6, 7.7**

  - [ ]* 2.3 Escribir property test para `MoneyCalculator` — Property 1: Subtotal consistente con items
    - **Property 1: subtotalCents = Σ(unitPriceCents × quantity) para lista arbitraria de SaleItems**
    - **Validates: Requirements 6.5, 7.1**

  - [ ]* 2.4 Escribir property test para `MoneyCalculator` — Property 2: Tax correcto sobre subtotal
    - **Property 2: taxCents = round(subtotalCents × taxRate) para subtotalCents ≥ 0 y taxRate ∈ [0,1]**
    - **Validates: Requirements 7.2**

  - [ ]* 2.5 Escribir property test para `MoneyCalculator` — Property 3: Total = Subtotal + Tax − Discount
    - **Property 3: totalCents = subtotalCents + taxCents − discountCents**
    - **Validates: Requirements 7.5**

  - [ ]* 2.6 Escribir property test para `MoneyCalculator` — Property 6: Descuento porcentual sobre subtotal
    - **Property 6: discountCents = round(subtotalCents × p / 100) para p ∈ [0,100]**
    - **Validates: Requirements 7.3**

  - [ ] 2.7 Crear entidades JPA: `Sale`, `SaleItem`, `Receipt`, `ReturnRecord`
    - Mapear columnas monetarias como BIGINT (centavos) y DECIMAL(19,2) (BigDecimal)
    - Definir enum `SaleStatus`: ACTIVE, COMPLETED, CANCELLED, FROZEN, RETURNED, PARTIALLY_RETURNED
    - Configurar relaciones JPA: `Sale` @OneToMany `SaleItem` con cascade ALL y orphanRemoval
    - Usar Jakarta Bean Validation en campos obligatorios
    - _Requirements: 15.2, 15.3, 15.5_

  - [ ] 2.8 Crear repositorios Spring Data JPA: `SaleRepository`, `SaleItemRepository`, `ReceiptRepository`, `ReturnRecordRepository`
    - Agregar query `findByStatusAndTerminalId` en `SaleRepository` para ventas congeladas
    - Agregar query `findBySaleIdAndProductId` en `ReturnRecordRepository` para validar devoluciones parciales
    - _Requirements: 15.2_

- [-] 3. Implementar autenticación JWT
  - [ ] 3.1 Crear DTOs de auth: `LoginRequest`, `LoginResponse`, `UserDto`
    - Validar campos con `@NotBlank`
    - _Requirements: 1.1_

  - [ ] 3.2 Implementar `AuthService` con usuarios en memoria (admin/cajero)
    - Hardcodear usuarios con BCryptPasswordEncoder
    - Generar JWT HS256 con claims: `sub`, `username`, `role`, `exp`
    - Retornar `LoginResponse` con token, role y UserDto
    - Lanzar excepción HTTP 401 para credenciales incorrectas y HTTP 403 para cuenta deshabilitada
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

  - [ ]* 3.3 Escribir property test para `AuthService` — Property 9: JWT contiene campos requeridos
    - **Property 9: Para cualquier login exitoso, el JWT contiene `sub`, `username`, `role` y `exp`**
    - **Validates: Requirements 1.2**

  - [ ] 3.4 Implementar `SecurityConfig` y filtro JWT
    - Configurar Spring Security: permitir POST /api/auth/login sin auth, proteger el resto con JWT
    - Implementar `JwtAuthenticationFilter` que valida el header `Authorization: Bearer <token>`
    - Retornar HTTP 401 para JWT inválido o ausente
    - _Requirements: 1.7, 1.8, 1.9_

  - [ ] 3.5 Implementar `CorsConfig`
    - Permitir origen `http://localhost:5173`, métodos GET/POST/PUT/DELETE/OPTIONS
    - Permitir headers `Content-Type` y `Authorization`
    - _Requirements: 2.2, 2.3_

  - [ ] 3.6 Crear `AuthController` con POST /api/auth/login
    - _Requirements: 1.1, 1.7_

  - [ ]* 3.7 Escribir tests unitarios para `AuthService`
    - Cubrir: login exitoso ADMIN, login exitoso CASHIER, credenciales incorrectas, cuenta deshabilitada
    - _Requirements: 1.1, 1.3, 1.4, 18.1_

- [x] 4. Checkpoint — Verificar autenticación base
  - Asegurar que todos los tests pasen. Consultar al usuario si surgen dudas.

- [-] 5. Implementar clientes HTTP externos
  - [ ] 5.1 Crear `RestTemplateConfig` con timeouts configurables para Product_API y Customer_API
    - Leer `product-api.timeout-ms` y `customer-api.timeout-ms` desde `application.yml`
    - _Requirements: 16.1, 16.4, 16.5_

  - [ ] 5.2 Implementar `ProductClientService`
    - Métodos: `searchByName`, `searchByBarcode`, `getProduct`, `decrementStock`, `incrementStock`
    - Manejar `HttpClientErrorException` (propagar 4xx), `HttpServerErrorException` (HTTP 503), `ResourceAccessException` (HTTP 503)
    - _Requirements: 3.1, 3.2, 3.4, 16.1, 16.2, 16.4_

  - [ ] 5.3 Implementar `CustomerClientService`
    - Métodos: `searchByName`, `searchByDocument`, `getCustomer`
    - Mismo manejo de errores que `ProductClientService`
    - _Requirements: 4.1, 4.2, 4.4, 16.1, 16.3, 16.4_

  - [ ] 5.4 Crear `ProductController` con GET /api/products/search (params: name, barcode)
    - Delegar a `ProductClientService`; requerir JWT
    - Incluir en respuesta: `productId`, `name`, `barcode`, `unitPrice`, `availableStock`, `category`
    - _Requirements: 3.1, 3.2, 3.3, 3.5_

  - [ ] 5.5 Crear `CustomerController` con GET /api/customers/search (params: name, document)
    - Delegar a `CustomerClientService`; requerir JWT
    - Incluir en respuesta: `customerId`, `fullName`, `documentType`, `documentNumber`, `creditStatus`
    - _Requirements: 4.1, 4.2, 4.3, 4.5_

  - [ ]* 5.6 Escribir tests unitarios para `ProductClientService` y `CustomerClientService`
    - Mockear RestTemplate; cubrir respuestas exitosas, errores 4xx/5xx y timeout
    - _Requirements: 16.2, 16.3, 16.4, 18.2_

- [ ] 6. Implementar `SaleService` — ciclo de vida de ventas
  - [ ] 6.1 Crear DTOs de venta: `CreateSaleRequest`, `AddItemRequest`, `UpdateItemRequest`, `CancelSaleRequest`, `SaleResponse`, `SaleItemDto`
    - Validar `terminalId` con `@NotBlank`
    - _Requirements: 5.6_

  - [ ] 6.2 Implementar creación de venta en `SaleService`
    - Generar UUID para `id`, extraer `cashierId` del JWT, inicializar totales en cero, estado ACTIVE
    - Asociar `customerId` si se proporciona
    - Persistir con `@Transactional`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 15.4_

  - [ ] 6.3 Implementar `addItem` en `SaleService`
    - Invocar `ProductClientService.getProduct` para obtener snapshot de precio
    - Si el producto ya existe en la Sale, incrementar cantidad (no crear nuevo item)
    - Validar quantity ≥ 1; validar stock disponible ≥ cantidad total del producto en la Sale
    - Recalcular totales con `MoneyCalculator.recalculateTotals`
    - Lanzar `SaleNotActiveException` si la Sale no está ACTIVE
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.8, 6.9_

  - [ ]* 6.4 Escribir property test para `SaleService` — Property 5: Producto duplicado incrementa cantidad
    - **Property 5: Agregar mismo productId a Sale ACTIVE no crea nuevo item; quantity = q1 + q2**
    - **Validates: Requirements 6.2**

  - [ ] 6.5 Implementar `updateItem` y `removeItem` en `SaleService`
    - Validar que la Sale esté ACTIVE antes de modificar
    - Recalcular totales tras cada operación
    - _Requirements: 6.5, 6.6, 6.7, 6.8_

  - [ ] 6.6 Implementar `cancelSale` en `SaleService`
    - Validar estado ACTIVE o FROZEN; validar `cancellationReason` no vacío y ≤ 255 chars
    - Cambiar estado a CANCELLED; no modificar stock
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

  - [ ] 6.7 Crear `SaleController` con endpoints: POST /api/sales, GET /api/sales/{saleId}, POST /api/sales/{saleId}/items, PUT /api/sales/{saleId}/items/{itemId}, DELETE /api/sales/{saleId}/items/{itemId}, POST /api/sales/{saleId}/cancel
    - _Requirements: 5.1, 6.1, 6.6, 6.7, 11.1_

  - [ ]* 6.8 Escribir tests unitarios para `SaleService`
    - Cubrir: creación, addItem (nuevo y duplicado), updateItem, removeItem, cancelación, validaciones de estado
    - Mockear `ProductClientService` con Mockito
    - _Requirements: 18.1, 18.2, 18.3, 18.4_

- [x] 7. Checkpoint — Verificar gestión de ventas e items
  - Asegurar que todos los tests pasen. Consultar al usuario si surgen dudas.

- [ ] 8. Implementar `FreezeService` y scheduler de expiración
  - [ ] 8.1 Implementar `FreezeService`: `freezeSale`, `resumeSale`, `getFrozenSales`, `expireOldFrozenSales`
    - `freezeSale`: validar estado ACTIVE, cambiar a FROZEN, registrar `frozenAt`
    - `resumeSale`: validar estado FROZEN, cambiar a ACTIVE
    - `getFrozenSales`: consultar por status=FROZEN y terminalId
    - `expireOldFrozenSales`: cancelar ventas FROZEN cuyo `frozenAt` supere `frozen-sale-expiration-hours`
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.7_

  - [ ] 8.2 Crear `FrozenSaleExpirationScheduler` con `@Scheduled`
    - Invocar `FreezeService.expireOldFrozenSales` con intervalo configurable (`frozen-sale-check-interval-ms`)
    - _Requirements: 12.6_

  - [ ] 8.3 Agregar endpoints de freeze a `SaleController` (o crear `FreezeController`): POST /api/sales/{saleId}/freeze, POST /api/sales/{saleId}/resume, GET /api/sales/frozen?terminalId=
    - _Requirements: 12.1, 12.3, 12.4_

  - [ ]* 8.4 Escribir tests unitarios para `FreezeService`
    - Cubrir: congelar exitoso, congelar venta no ACTIVE, reanudar exitoso, reanudar venta no FROZEN, expiración automática
    - _Requirements: 18.1, 18.3, 18.4_

- [ ] 9. Implementar `ReceiptService`
  - [ ] 9.1 Implementar `ReceiptService`: `generateSaleReceipt`, `generateReturnReceipt`, `getReceipt`
    - `generateSaleReceipt`: generar Transaction_ID único (UUID), serializar recibo completo a JSON, persistir en `receipts`
    - Incluir en recibo: nombre tienda, terminalId, cashierId, fecha/hora, cliente (si aplica), items, subtotal, tax, discount, total, paymentType, amountReceived (CASH), cambio (CASH), Transaction_ID
    - `generateReturnReceipt`: incluir Transaction_ID original, fecha/hora devolución, items devueltos, total devuelto, razón
    - `getReceipt`: buscar por transactionId; lanzar `ReceiptNotFoundException` si no existe
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ]* 9.2 Escribir property test para `ReceiptService` — Property 10: Recibo contiene campos obligatorios
    - **Property 10: Para cualquier Sale completada, el recibo contiene transactionId, terminalId, cashierId, items, subtotal, tax, total, paymentType y generatedAt**
    - **Validates: Requirements 10.1, 10.2**

  - [ ] 9.3 Crear `ReceiptController` con GET /api/receipts/{transactionId}
    - _Requirements: 10.5_

  - [ ]* 9.4 Escribir tests unitarios para `ReceiptService`
    - Cubrir: recibo CASH, recibo CREDIT, recibo de devolución, consulta por transactionId, transactionId no encontrado
    - _Requirements: 18.1_

- [ ] 10. Implementar `PaymentService` — checkout CASH y CREDIT
  - [ ] 10.1 Crear DTOs de checkout: `CheckoutRequest`, `ReceiptResponse`
    - Validar `paymentType` con `@NotBlank`
    - _Requirements: 8.1, 9.1_

  - [ ] 10.2 Implementar checkout CASH en `PaymentService`
    - Validar: Sale tiene items, `amountReceived` ≥ total, stock disponible para cada item
    - Cambiar estado a COMPLETED, decrementar stock en `ProductClientService`, calcular cambio
    - Delegar generación de recibo a `ReceiptService`
    - Usar `@Transactional`
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 15.4_

  - [ ]* 10.3 Escribir property test para `PaymentService` — Property 7: Cambio CASH ≥ 0
    - **Property 7: Para cualquier totalCents ≥ 0 y amountReceivedCents ≥ totalCents, el cambio = amountReceivedCents − totalCents ≥ 0**
    - **Validates: Requirements 8.2, 8.4**

  - [ ] 10.4 Implementar checkout CREDIT en `PaymentService`
    - Validar: Sale tiene `customerId`, `creditStatus` del cliente es APPROVED, stock disponible
    - Generar `creditReferenceNumber` único, cambiar estado a COMPLETED, decrementar stock
    - Delegar generación de recibo a `ReceiptService`
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

  - [ ] 10.5 Agregar endpoint de checkout a `SaleController` (o crear `CheckoutController`): POST /api/sales/{saleId}/checkout
    - _Requirements: 8.5, 9.4_

  - [ ]* 10.6 Escribir tests unitarios para `PaymentService`
    - Cubrir: checkout CASH exitoso, monto insuficiente, sin items, stock insuficiente; checkout CREDIT exitoso, sin cliente, crédito no aprobado
    - Mockear `ProductClientService` y `CustomerClientService`
    - _Requirements: 18.1, 18.2, 18.4_

- [x] 11. Checkpoint — Verificar checkout y recibos
  - Asegurar que todos los tests pasen. Consultar al usuario si surgen dudas.

- [ ] 12. Implementar `ReturnService` — devoluciones totales y parciales
  - [ ] 12.1 Crear DTOs de devolución: `ReturnRequest`, `PartialReturnRequest`, `ReturnItemRequest`
    - Validar `returnReason` con `@NotBlank`
    - _Requirements: 13.3, 14.4_

  - [ ] 12.2 Implementar `fullReturn` en `ReturnService`
    - Validar estado COMPLETED; validar que no esté ya en RETURNED
    - Cambiar estado a RETURNED, incrementar stock de todos los items en `ProductClientService`
    - Generar nota de crédito si `paymentType` es CREDIT (referenciar `creditReferenceNumber`)
    - Delegar generación de recibo de devolución a `ReceiptService`
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6_

  - [ ] 12.3 Implementar `partialReturn` en `ReturnService`
    - Validar estado COMPLETED o PARTIALLY_RETURNED
    - Validar que cantidad devuelta por item no exceda (comprada − ya devuelta)
    - Cambiar estado a PARTIALLY_RETURNED, incrementar stock solo de items devueltos
    - Persistir `ReturnRecord` por cada item devuelto
    - Generar nota de crédito parcial si `paymentType` es CREDIT
    - Permitir múltiples devoluciones parciales mientras queden items no devueltos
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7_

  - [ ]* 12.4 Escribir property test para `ReturnService` — Property 8: Devolución parcial no excede cantidad comprada
    - **Property 8: Σ quantityReturned (ReturnRecords existentes) + nueva cantidad ≤ quantity original del producto**
    - **Validates: Requirements 14.2**

  - [ ] 12.5 Agregar endpoints de devolución a `SaleController` (o crear `ReturnController`): POST /api/sales/{saleId}/return, POST /api/sales/{saleId}/return/partial
    - _Requirements: 13.1, 14.1_

  - [ ]* 12.6 Escribir tests unitarios para `ReturnService`
    - Cubrir: devolución total exitosa, venta no COMPLETED, ya devuelta; devolución parcial exitosa, excede cantidad, razón faltante, múltiples devoluciones parciales
    - _Requirements: 18.1, 18.3, 18.4_

- [ ] 13. Implementar manejo global de errores y jerarquía de excepciones
  - [ ] 13.1 Crear jerarquía de excepciones personalizadas
    - Implementar: `SaleNotFoundException`, `ReceiptNotFoundException`, `SaleNotActiveException`, `SaleNotFrozenException`, `SaleNotCompletedException`, `SaleAlreadyReturnedException`, `InsufficientStockException`, `InsufficientPaymentException`, `InvalidQuantityException`, `CreditNotApprovedException`, `CustomerRequiredException`, `EmptySaleException`, `ExternalServiceException`, `ValidationException`
    - _Requirements: 20.1_

  - [ ] 13.2 Implementar `GlobalExceptionHandler` con `@ControllerAdvice`
    - Capturar todas las excepciones personalizadas y retornar JSON con `message`, `timestamp` (ISO-8601) y `path`
    - Manejar `MethodArgumentNotValidException` (Bean Validation) con HTTP 400 y lista de errores por campo
    - Manejar `NoResourceFoundException` / `NoHandlerFoundException` con HTTP 404
    - Manejar `HttpRequestMethodNotSupportedException` con HTTP 405
    - Registrar errores 5xx con nivel ERROR incluyendo stack trace
    - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.5_

  - [ ]* 13.3 Escribir tests unitarios para `GlobalExceptionHandler`
    - Cubrir: cada tipo de excepción personalizada, errores de validación, 404, 405
    - _Requirements: 20.1, 20.2, 20.3, 20.4_

- [x] 14. Configurar OpenAPI / Swagger UI
  - Agregar `OpenApiConfig` con metadatos de la API (título, versión, descripción)
  - Verificar que `/v3/api-docs` y `/swagger-ui.html` estén accesibles
  - Excluir rutas de documentación del filtro JWT en `SecurityConfig`
  - _Requirements: 17.1, 17.2, 17.3_

- [ ] 15. Escribir tests de integración con H2 y WireMock
  - [ ] 15.1 Configurar base de tests de integración con `@SpringBootTest`, H2 y WireMock
    - Crear `application-test.yml` con H2 en memoria y URLs de WireMock para Product_API y Customer_API
    - _Requirements: 19.1_

  - [ ]* 15.2 Test de integración: flujo completo CASH
    - Crear venta → agregar items → checkout CASH → verificar recibo y Transaction_ID
    - _Requirements: 19.2_

  - [ ]* 15.3 Test de integración: flujo completo CREDIT
    - Crear venta → asociar cliente → agregar items → checkout CREDIT → verificar Credit_Reference
    - _Requirements: 19.3_

  - [ ]* 15.4 Test de integración: flujo freeze
    - Crear venta → agregar items → congelar → reanudar → checkout
    - _Requirements: 19.4_

  - [ ]* 15.5 Test de integración: flujo devolución total
    - Completar venta → devolución total → verificar stock restaurado en WireMock
    - _Requirements: 19.5_

  - [ ]* 15.6 Test de integración: flujo devolución parcial
    - Completar venta → devolver 2 de 5 items → verificar stock parcialmente restaurado
    - _Requirements: 19.6_

  - [ ]* 15.7 Test de integración: flujo cancelación
    - Crear venta → agregar items → cancelar → verificar que no se puede modificar (HTTP 422)
    - _Requirements: 19.7_

- [x] 16. Checkpoint final — Verificar cobertura y calidad
  - Ejecutar `mvn verify` para compilar, correr todos los tests y verificar cobertura JaCoCo (90% servicios, 80% proyecto)
  - Asegurar que todos los tests pasen. Consultar al usuario si surgen dudas.

## Notes

- Las tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido
- Cada tarea referencia los requisitos específicos para trazabilidad
- Los checkpoints validan el progreso incremental antes de continuar
- Los property tests usan **jqwik** con mínimo 100 iteraciones por propiedad
- Los tests de integración usan **WireMock** para simular Product_API y Customer_API de forma determinista
- Todos los cálculos monetarios intermedios se realizan en centavos (long); solo se convierte a BigDecimal(2) al construir respuestas
- El scheduler de expiración de ventas congeladas se configura con `@Scheduled` y el intervalo `frozen-sale-check-interval-ms`
