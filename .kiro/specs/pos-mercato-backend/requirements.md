# Requirements Document

## Introduction

El sistema **POS MERCATO Backend** es una API REST desarrollada en Java Spring Boot que gestiona las transacciones de venta en los terminales de punto de venta (POS) de un supermercado. El backend expone endpoints consumidos por un frontend React/TypeScript ya existente y se integra con dos APIs externas: la **Product API** (catálogo de productos, stock y precios) y la **Customer API** (registro de clientes y estado de crédito).

El sistema cubre el ciclo de vida completo de una venta: autenticación de cajeros, búsqueda de productos y clientes, creación y gestión de ventas, procesamiento de pagos en efectivo y a crédito, congelamiento de ventas, cancelaciones, devoluciones totales y parciales, y generación de recibos. Todos los cálculos monetarios se realizan con precisión de centavos (aritmética entera) y se exponen con `BigDecimal` de 2 decimales.

---

## Glossary

- **Sales_API**: El backend Spring Boot que se está construyendo; gestiona transacciones de venta en el POS.
- **Product_API**: API externa existente que provee el catálogo de productos, stock disponible y precios.
- **Customer_API**: API externa existente que provee el registro de clientes y su estado de crédito.
- **Auth_Service**: Componente interno del Sales_API responsable de autenticación y emisión de JWT.
- **Sale_Service**: Componente interno del Sales_API responsable de la lógica de negocio de ventas.
- **Payment_Service**: Componente interno del Sales_API responsable de procesar pagos en efectivo y a crédito.
- **Return_Service**: Componente interno del Sales_API responsable de procesar devoluciones totales y parciales.
- **Receipt_Service**: Componente interno del Sales_API responsable de generar recibos de venta y devolución.
- **Freeze_Service**: Componente interno del Sales_API responsable de congelar, reanudar y expirar ventas.
- **Cashier**: Usuario autenticado con rol CASHIER que opera el terminal POS.
- **Admin**: Usuario autenticado con rol ADMIN con acceso completo al sistema.
- **Terminal**: Dispositivo POS identificado por un `terminalId` único.
- **Sale**: Entidad que representa una transacción de venta en un Terminal.
- **SaleItem**: Línea de producto dentro de una Sale, con snapshot de precio al momento de agregar.
- **Receipt**: Documento generado al completar o devolver una Sale.
- **Transaction_ID**: Identificador único generado al completar una Sale exitosamente.
- **Credit_Reference**: Número de referencia auto-generado para ventas a crédito.
- **JWT**: JSON Web Token firmado con HS256 que contiene `sub`, `username`, `role` y `exp`.
- **ACTIVE**: Estado inicial de una Sale recién creada.
- **COMPLETED**: Estado de una Sale que ha pasado por checkout exitoso.
- **CANCELLED**: Estado de una Sale cancelada antes del checkout.
- **FROZEN**: Estado de una Sale pausada temporalmente por el Cashier.
- **RETURNED**: Estado de una Sale completamente devuelta.
- **PARTIALLY_RETURNED**: Estado de una Sale con devolución parcial de items.
- **CASH**: Tipo de pago en efectivo.
- **CREDIT**: Tipo de pago a crédito.
- **Subtotal**: Suma de (precio_unitario × cantidad) de todos los SaleItems.
- **Tax**: Impuesto calculado como Subtotal × tax_rate (configurable, default 19%).
- **Discount**: Descuento opcional, porcentaje o monto fijo aplicado a la Sale.
- **Total**: Resultado de Subtotal + Tax − Discount.

---

## Requirements

### Requirement 1: Autenticación de Usuarios

**User Story:** Como Cashier o Admin, quiero autenticarme con mis credenciales, para que el sistema me identifique y me otorgue un JWT con mi rol.

#### Acceptance Criteria

1. WHEN el Auth_Service recibe una solicitud POST /api/auth/login con `username` y `password` válidos, THE Auth_Service SHALL retornar HTTP 200 con un cuerpo JSON que contenga `token` (JWT firmado HS256), `role` (CASHIER o ADMIN) y `user` (objeto con `id`, `username` y `displayName`).
2. THE Auth_Service SHALL incluir en el payload del JWT los campos `sub`, `username`, `role` y `exp` (Unix timestamp en segundos).
3. WHEN el Auth_Service recibe credenciales incorrectas, THE Auth_Service SHALL retornar HTTP 401 con el cuerpo `{"message": "Credenciales incorrectas"}`.
4. WHEN el Auth_Service recibe una solicitud de un usuario con cuenta deshabilitada, THE Auth_Service SHALL retornar HTTP 403 con el cuerpo `{"message": "Acceso denegado"}`.
5. THE Auth_Service SHALL reconocer al usuario `admin` con contraseña `admin123`, rol ADMIN y displayName "Administrador".
6. THE Auth_Service SHALL reconocer al usuario `cajero` con contraseña `cajero123`, rol CASHIER y displayName "Cajero 1".
7. THE Sales_API SHALL exponer el endpoint POST /api/auth/login sin requerir autenticación previa.
8. THE Sales_API SHALL proteger todos los endpoints excepto POST /api/auth/login con validación de JWT en el header `Authorization: Bearer <token>`.
9. IF un request llega a un endpoint protegido sin JWT válido, THEN THE Sales_API SHALL retornar HTTP 401.

---

### Requirement 2: Configuración CORS y Servidor

**User Story:** Como desarrollador del frontend, quiero que el backend acepte solicitudes desde el servidor de desarrollo Vite, para que la integración funcione sin errores de CORS.

#### Acceptance Criteria

1. THE Sales_API SHALL escuchar en el puerto 8080.
2. THE Sales_API SHALL permitir solicitudes CORS desde el origen `http://localhost:5173` con los métodos GET, POST, PUT, DELETE y OPTIONS.
3. THE Sales_API SHALL permitir los headers `Content-Type` y `Authorization` en solicitudes CORS.
4. THE Sales_API SHALL responder con `Content-Type: application/json` en todos los endpoints REST.

---

### Requirement 3: Búsqueda de Productos

**User Story:** Como Cashier, quiero buscar productos por nombre o código de barras, para que pueda agregarlos rápidamente a una venta.

#### Acceptance Criteria

1. WHEN el Sales_API recibe GET /api/products/search con el parámetro `name`, THE Sale_Service SHALL invocar la Product_API con búsqueda parcial e insensible a mayúsculas y retornar la lista de productos coincidentes.
2. WHEN el Sales_API recibe GET /api/products/search con el parámetro `barcode`, THE Sale_Service SHALL invocar la Product_API con coincidencia exacta de barcode y retornar el producto correspondiente.
3. THE Sales_API SHALL incluir en cada producto de la respuesta: `productId`, `name`, `barcode`, `unitPrice`, `availableStock` y `category`.
4. IF la Product_API no está disponible, THEN THE Sales_API SHALL retornar HTTP 503 con el mensaje `{"message": "Servicio de productos no disponible"}`.
5. THE Sales_API SHALL requerir autenticación JWT válida para acceder a los endpoints de búsqueda de productos.

---

### Requirement 4: Búsqueda de Clientes

**User Story:** Como Cashier, quiero buscar clientes por nombre o número de documento, para que pueda asociarlos a una venta a crédito.

#### Acceptance Criteria

1. WHEN el Sales_API recibe GET /api/customers/search con el parámetro `name`, THE Sale_Service SHALL invocar la Customer_API con búsqueda parcial por nombre y retornar la lista de clientes coincidentes.
2. WHEN el Sales_API recibe GET /api/customers/search con el parámetro `document`, THE Sale_Service SHALL invocar la Customer_API con coincidencia exacta de número de documento y retornar el cliente correspondiente.
3. THE Sales_API SHALL incluir en cada cliente de la respuesta: `customerId`, `fullName`, `documentType`, `documentNumber` y `creditStatus` (APPROVED, REJECTED o PENDING).
4. IF la Customer_API no está disponible, THEN THE Sales_API SHALL retornar HTTP 503 con el mensaje `{"message": "Servicio de clientes no disponible"}`.
5. THE Sales_API SHALL requerir autenticación JWT válida para acceder a los endpoints de búsqueda de clientes.

---

### Requirement 5: Creación de Ventas

**User Story:** Como Cashier, quiero crear una nueva venta asociada a mi terminal, para que pueda comenzar a registrar productos.

#### Acceptance Criteria

1. WHEN el Sale_Service recibe una solicitud de creación de venta con `terminalId` y `cashierId`, THE Sale_Service SHALL crear una Sale con estado ACTIVE y retornar HTTP 201 con el objeto Sale creado.
2. THE Sale_Service SHALL registrar el `cashierId` extraído del JWT del Cashier autenticado.
3. WHERE se proporciona un `customerId` en la solicitud de creación, THE Sale_Service SHALL asociar el cliente a la Sale.
4. THE Sale_Service SHALL asignar un identificador único a cada Sale en el momento de su creación.
5. THE Sale_Service SHALL inicializar los totales de la Sale (subtotal, tax, discount, total) en cero al momento de la creación.
6. IF el `terminalId` no es proporcionado en la solicitud, THEN THE Sales_API SHALL retornar HTTP 400 con un mensaje de validación.

---

### Requirement 6: Gestión de Items en una Venta

**User Story:** Como Cashier, quiero agregar, actualizar y eliminar productos en una venta activa, para que el carrito refleje exactamente lo que el cliente desea comprar.

#### Acceptance Criteria

1. WHEN el Sale_Service recibe una solicitud de agregar item con `productId` o `barcode` y `quantity`, THE Sale_Service SHALL invocar la Product_API para obtener el producto y agregar un SaleItem a la Sale con snapshot de `unitPrice`, `productName` y `productId`.
2. WHEN se agrega un producto que ya existe en la Sale, THE Sale_Service SHALL incrementar la cantidad del SaleItem existente en lugar de crear uno nuevo.
3. THE Sale_Service SHALL validar que la `quantity` solicitada sea mayor o igual a 1; IF la cantidad es menor a 1, THEN THE Sales_API SHALL retornar HTTP 400.
4. WHEN se agrega un item, THE Sale_Service SHALL invocar la Product_API para verificar que el `availableStock` sea mayor o igual a la cantidad total del producto en la Sale; IF el stock es insuficiente, THEN THE Sales_API SHALL retornar HTTP 409 con el mensaje `{"message": "Stock insuficiente", "productId": "<id>", "availableStock": <n>}`.
5. WHEN se agrega, actualiza o elimina un SaleItem, THE Sale_Service SHALL recalcular el Subtotal, Tax, Discount y Total de la Sale.
6. WHEN el Sale_Service recibe una solicitud de actualizar la cantidad de un SaleItem existente, THE Sale_Service SHALL actualizar la cantidad y recalcular los totales.
7. WHEN el Sale_Service recibe una solicitud de eliminar un SaleItem, THE Sale_Service SHALL remover el item de la Sale y recalcular los totales.
8. IF se intenta modificar una Sale que no está en estado ACTIVE, THEN THE Sales_API SHALL retornar HTTP 422 con el mensaje `{"message": "La venta no está activa"}`.
9. THE Sale_Service SHALL requerir autenticación JWT válida para todas las operaciones de gestión de items.

---

### Requirement 7: Cálculo de Totales

**User Story:** Como Cashier, quiero que los totales de la venta se calculen automáticamente con precisión, para que el monto cobrado sea correcto.

#### Acceptance Criteria

1. THE Sale_Service SHALL calcular el Subtotal como la suma de (precio_unitario × cantidad) de todos los SaleItems, usando aritmética entera en centavos internamente.
2. THE Sale_Service SHALL calcular el Tax como Subtotal × tax_rate, donde tax_rate es configurable con valor por defecto de 19%.
3. WHERE se aplica un Discount de tipo porcentaje, THE Sale_Service SHALL calcular el Discount como Subtotal × porcentaje_descuento.
4. WHERE se aplica un Discount de tipo monto fijo, THE Sale_Service SHALL usar el monto fijo como Discount.
5. THE Sale_Service SHALL calcular el Total como Subtotal + Tax − Discount.
6. THE Sale_Service SHALL representar todos los valores monetarios en la respuesta JSON como `BigDecimal` con exactamente 2 decimales de precisión.
7. THE Sale_Service SHALL realizar todos los cálculos intermedios en centavos (enteros) para evitar errores de punto flotante, convirtiendo a `BigDecimal` de 2 decimales solo al construir la respuesta.

---

### Requirement 8: Checkout — Pago en Efectivo

**User Story:** Como Cashier, quiero procesar el pago en efectivo de una venta, para que la transacción quede completada y se genere el recibo.

#### Acceptance Criteria

1. WHEN el Payment_Service recibe una solicitud de checkout con `paymentType: CASH` y `amountReceived`, THE Payment_Service SHALL validar que la Sale tenga al menos un SaleItem; IF no tiene items, THEN THE Sales_API SHALL retornar HTTP 422 con el mensaje `{"message": "La venta no tiene items"}`.
2. WHEN el Payment_Service procesa un pago CASH, THE Payment_Service SHALL validar que `amountReceived` sea mayor o igual al Total de la Sale; IF es menor, THEN THE Sales_API SHALL retornar HTTP 422 con el mensaje `{"message": "Monto recibido insuficiente"}`.
3. WHEN el Payment_Service valida el stock en checkout, THE Payment_Service SHALL invocar la Product_API para verificar disponibilidad de cada item; IF algún producto tiene stock insuficiente, THEN THE Sales_API SHALL retornar HTTP 409 con la lista de productos sin stock.
4. WHEN el checkout CASH es exitoso, THE Payment_Service SHALL cambiar el estado de la Sale a COMPLETED, decrementar el stock de cada item en la Product_API, calcular el cambio como `amountReceived − Total`, y delegar al Receipt_Service la generación del recibo.
5. WHEN el checkout CASH es exitoso, THE Sales_API SHALL retornar HTTP 200 con el recibo generado que incluye el Transaction_ID único.
6. THE Payment_Service SHALL requerir autenticación JWT válida para procesar pagos.

---

### Requirement 9: Checkout — Pago a Crédito

**User Story:** Como Cashier, quiero procesar el pago a crédito de una venta, para que el cliente pueda pagar con su línea de crédito aprobada.

#### Acceptance Criteria

1. WHEN el Payment_Service recibe una solicitud de checkout con `paymentType: CREDIT`, THE Payment_Service SHALL validar que la Sale tenga un `customerId` asociado; IF no tiene cliente, THEN THE Sales_API SHALL retornar HTTP 422 con el mensaje `{"message": "Venta a crédito requiere cliente asociado"}`.
2. WHEN el Payment_Service procesa un pago CREDIT, THE Payment_Service SHALL invocar la Customer_API para obtener el `creditStatus` del cliente; IF el `creditStatus` no es APPROVED, THEN THE Sales_API SHALL retornar HTTP 422 con el mensaje `{"message": "El cliente no tiene crédito aprobado"}`.
3. WHEN el checkout CREDIT es exitoso, THE Payment_Service SHALL generar un `creditReferenceNumber` único, cambiar el estado de la Sale a COMPLETED, decrementar el stock en la Product_API, y delegar al Receipt_Service la generación del recibo.
4. WHEN el checkout CREDIT es exitoso, THE Sales_API SHALL retornar HTTP 200 con el recibo generado que incluye el Transaction_ID y el `creditReferenceNumber`.
5. WHEN el Payment_Service valida el stock en checkout CREDIT, THE Payment_Service SHALL aplicar las mismas validaciones de stock que el checkout CASH (Requirement 8, criterio 3).

---

### Requirement 10: Generación de Recibos

**User Story:** Como Cashier, quiero que el sistema genere un recibo detallado al completar una venta, para que el cliente tenga constancia de su compra.

#### Acceptance Criteria

1. WHEN el Receipt_Service genera un recibo de venta, THE Receipt_Service SHALL incluir: nombre de la tienda, `terminalId`, `cashierId`, fecha y hora de la transacción, información del cliente (si aplica), lista de todos los SaleItems con nombre, cantidad, precio unitario y total de línea, Subtotal, Tax, Discount, Total, método de pago, `amountReceived` (solo CASH), cambio (solo CASH), y Transaction_ID único.
2. THE Receipt_Service SHALL generar un Transaction_ID único para cada recibo de venta completada.
3. WHEN el Receipt_Service genera un recibo de devolución, THE Receipt_Service SHALL incluir: Transaction_ID original, fecha y hora de la devolución, lista de items devueltos con cantidades y precios, total devuelto, y razón de devolución.
4. THE Receipt_Service SHALL persistir cada recibo generado en la base de datos para consulta posterior.
5. THE Sales_API SHALL exponer GET /api/receipts/{transactionId} para consultar un recibo por su Transaction_ID; IF el recibo no existe, THEN THE Sales_API SHALL retornar HTTP 404.

---

### Requirement 11: Cancelación de Ventas

**User Story:** Como Cashier, quiero cancelar una venta antes del checkout, para que pueda abortar una transacción errónea sin afectar el inventario.

#### Acceptance Criteria

1. WHEN el Sale_Service recibe una solicitud de cancelación con `cancellationReason`, THE Sale_Service SHALL validar que la Sale esté en estado ACTIVE o FROZEN; IF está en otro estado, THEN THE Sales_API SHALL retornar HTTP 422 con el mensaje `{"message": "Solo se pueden cancelar ventas ACTIVE o FROZEN"}`.
2. WHEN la cancelación es válida, THE Sale_Service SHALL cambiar el estado de la Sale a CANCELLED y registrar el `cancellationReason`.
3. THE Sale_Service SHALL validar que `cancellationReason` no esté vacío y tenga como máximo 255 caracteres; IF no cumple, THEN THE Sales_API SHALL retornar HTTP 400.
4. IF se intenta modificar, agregar items o completar una Sale en estado CANCELLED, THEN THE Sales_API SHALL retornar HTTP 422 con el mensaje `{"message": "La venta está cancelada"}`.
5. THE Sale_Service SHALL retornar HTTP 200 con la Sale actualizada al estado CANCELLED.
6. THE Sale_Service SHALL confirmar que no se realizan cambios de stock al cancelar una venta.

---

### Requirement 12: Congelamiento de Ventas (Hold)

**User Story:** Como Cashier, quiero pausar una venta activa y reanudarla después, para que pueda atender a otro cliente sin perder el carrito actual.

#### Acceptance Criteria

1. WHEN el Freeze_Service recibe una solicitud de congelar una Sale, THE Freeze_Service SHALL validar que la Sale esté en estado ACTIVE; IF está en otro estado, THEN THE Sales_API SHALL retornar HTTP 422 con el mensaje `{"message": "Solo se pueden congelar ventas ACTIVE"}`.
2. WHEN el congelamiento es válido, THE Freeze_Service SHALL cambiar el estado de la Sale a FROZEN, retener todos los SaleItems y totales, y registrar la fecha y hora de congelamiento.
3. WHEN el Freeze_Service recibe una solicitud de reanudar una Sale FROZEN, THE Freeze_Service SHALL cambiar el estado de la Sale a ACTIVE.
4. THE Sales_API SHALL exponer GET /api/sales/frozen?terminalId={id} para listar todas las ventas en estado FROZEN de un Terminal específico.
5. THE Freeze_Service SHALL configurar un tiempo de expiración para ventas FROZEN con valor por defecto de 2 horas; WHEN una Sale FROZEN supera el tiempo de expiración, THE Freeze_Service SHALL cambiar automáticamente su estado a CANCELLED con razón "Venta congelada expirada".
6. THE Freeze_Service SHALL ejecutar la verificación de expiración de ventas FROZEN de forma periódica (scheduler configurable).
7. IF se intenta reanudar una Sale que no está en estado FROZEN, THEN THE Sales_API SHALL retornar HTTP 422 con el mensaje `{"message": "La venta no está congelada"}`.

---

### Requirement 13: Devolución Total

**User Story:** Como Cashier, quiero procesar la devolución completa de una venta, para que el cliente reciba el reembolso de todos sus productos.

#### Acceptance Criteria

1. WHEN el Return_Service recibe una solicitud de devolución total con `returnReason`, THE Return_Service SHALL validar que la Sale esté en estado COMPLETED; IF está en otro estado, THEN THE Sales_API SHALL retornar HTTP 422 con el mensaje `{"message": "Solo se pueden devolver ventas COMPLETED"}`.
2. WHEN la devolución total es válida, THE Return_Service SHALL cambiar el estado de la Sale a RETURNED, incrementar el stock de todos los SaleItems en la Product_API, y delegar al Receipt_Service la generación del recibo de devolución.
3. THE Return_Service SHALL validar que `returnReason` no esté vacío; IF está vacío, THEN THE Sales_API SHALL retornar HTTP 400.
4. IF se intenta devolver una Sale que ya está en estado RETURNED, THEN THE Sales_API SHALL retornar HTTP 422 con el mensaje `{"message": "La venta ya fue devuelta"}`.
5. WHEN la Sale es de tipo CREDIT, THE Return_Service SHALL generar una nota de crédito referenciando el `creditReferenceNumber` original.
6. THE Return_Service SHALL retornar HTTP 200 con el recibo de devolución generado.

---

### Requirement 14: Devolución Parcial

**User Story:** Como Cashier, quiero procesar la devolución de items específicos de una venta, para que el cliente reciba el reembolso solo de los productos que devuelve.

#### Acceptance Criteria

1. WHEN el Return_Service recibe una solicitud de devolución parcial con lista de items y cantidades, THE Return_Service SHALL validar que la Sale esté en estado COMPLETED o PARTIALLY_RETURNED; IF está en otro estado, THEN THE Sales_API SHALL retornar HTTP 422 con el mensaje `{"message": "Solo se pueden devolver ventas COMPLETED o PARTIALLY_RETURNED"}`.
2. THE Return_Service SHALL validar que la cantidad devuelta de cada item no exceda la cantidad originalmente comprada menos las cantidades ya devueltas en devoluciones parciales anteriores; IF excede, THEN THE Sales_API SHALL retornar HTTP 422 con el mensaje `{"message": "Cantidad devuelta excede la cantidad comprada para el producto <productId>"}`.
3. WHEN la devolución parcial es válida, THE Return_Service SHALL cambiar el estado de la Sale a PARTIALLY_RETURNED, incrementar el stock solo de los items devueltos en la Product_API, y delegar al Receipt_Service la generación del recibo de devolución parcial.
4. THE Return_Service SHALL requerir `returnReason` por cada item devuelto; IF falta la razón de algún item, THEN THE Sales_API SHALL retornar HTTP 400.
5. WHEN la Sale es de tipo CREDIT, THE Return_Service SHALL generar una nota de crédito parcial referenciando el `creditReferenceNumber` original.
6. THE Return_Service SHALL retornar HTTP 200 con el recibo de devolución parcial generado.
7. THE Return_Service SHALL permitir múltiples devoluciones parciales sobre la misma Sale en estado PARTIALLY_RETURNED, siempre que queden items no devueltos.

---

### Requirement 15: Persistencia y Esquema de Base de Datos

**User Story:** Como desarrollador, quiero que el sistema persista todas las entidades en una base de datos relacional, para que los datos sobrevivan reinicios del servidor.

#### Acceptance Criteria

1. THE Sales_API SHALL usar H2 en modo embebido para los perfiles `dev` y `test`, con un esquema compatible con PostgreSQL.
2. THE Sales_API SHALL persistir las entidades: Sale, SaleItem, Receipt, y ReturnRecord usando Spring Data JPA / Hibernate.
3. THE Sales_API SHALL usar Jakarta Bean Validation para validar los campos de las entidades antes de persistirlos.
4. THE Sales_API SHALL gestionar las transacciones de base de datos con `@Transactional` en los métodos de servicio que realizan múltiples operaciones de escritura.
5. THE Sales_API SHALL usar `BigDecimal` como tipo de columna para todos los campos monetarios en la base de datos.

---

### Requirement 16: Integración con APIs Externas

**User Story:** Como desarrollador, quiero que el sistema se integre de forma robusta con la Product API y la Customer API, para que los fallos externos no corrompan el estado interno.

#### Acceptance Criteria

1. THE Sales_API SHALL usar RestTemplate o WebClient para realizar llamadas HTTP a la Product_API y la Customer_API.
2. IF la Product_API retorna un error HTTP 4xx o 5xx, THEN THE Sales_API SHALL propagar el error apropiado al cliente con el código HTTP correspondiente.
3. IF la Customer_API retorna un error HTTP 4xx o 5xx, THEN THE Sales_API SHALL propagar el error apropiado al cliente con el código HTTP correspondiente.
4. IF la Product_API o la Customer_API no responden dentro del timeout configurado, THEN THE Sales_API SHALL retornar HTTP 503.
5. THE Sales_API SHALL externalizar las URLs base de la Product_API y la Customer_API como propiedades de configuración (`product-api.base-url`, `customer-api.base-url`).

---

### Requirement 17: Documentación de la API

**User Story:** Como desarrollador del frontend, quiero acceder a la documentación interactiva de la API, para que pueda explorar y probar los endpoints fácilmente.

#### Acceptance Criteria

1. THE Sales_API SHALL exponer la documentación OpenAPI 3.0 en la ruta `/v3/api-docs` usando SpringDoc.
2. THE Sales_API SHALL exponer la interfaz Swagger UI en la ruta `/swagger-ui.html`.
3. THE Sales_API SHALL documentar todos los endpoints con sus parámetros de entrada, respuestas posibles y códigos HTTP.

---

### Requirement 18: Testing Unitario

**User Story:** Como desarrollador, quiero una suite de tests unitarios completa, para que cada regla de negocio esté verificada de forma aislada.

#### Acceptance Criteria

1. THE Sales_API SHALL incluir tests unitarios con JUnit 5 y Mockito para cada método público de Auth_Service, Sale_Service, Payment_Service, Return_Service, Freeze_Service y Receipt_Service.
2. THE Sales_API SHALL mockear todas las llamadas a Product_API y Customer_API en los tests unitarios usando Mockito.
3. THE Sales_API SHALL incluir tests unitarios que cubran todas las transiciones de estado: ACTIVE → COMPLETED, ACTIVE → FROZEN → ACTIVE, ACTIVE → CANCELLED, FROZEN → CANCELLED, COMPLETED → RETURNED, COMPLETED → PARTIALLY_RETURNED.
4. THE Sales_API SHALL incluir tests unitarios para casos borde: checkout de venta sin items, devolución que excede cantidad comprada, congelamiento de venta ya congelada, venta a crédito sin cliente asociado, pago en efectivo con monto insuficiente.
5. THE Sales_API SHALL alcanzar una cobertura mínima del 90% de líneas en la capa de servicios, medida con JaCoCo.

---

### Requirement 19: Testing de Integración

**User Story:** Como desarrollador, quiero tests de integración que validen los flujos completos de negocio, para que la interacción entre componentes esté verificada.

#### Acceptance Criteria

1. THE Sales_API SHALL incluir tests de integración con `@SpringBootTest`, base de datos H2 y WireMock o MockRestServiceServer para simular la Product_API y la Customer_API.
2. THE Sales_API SHALL incluir un test de integración para el flujo completo de venta en efectivo: crear venta → agregar items → checkout CASH → verificar recibo y Transaction_ID.
3. THE Sales_API SHALL incluir un test de integración para el flujo completo de venta a crédito: crear venta → asociar cliente → agregar items → checkout CREDIT → verificar Credit_Reference.
4. THE Sales_API SHALL incluir un test de integración para el flujo de congelamiento: crear venta → agregar items → congelar → reanudar → checkout.
5. THE Sales_API SHALL incluir un test de integración para el flujo de devolución total: completar venta → devolución total → verificar stock restaurado.
6. THE Sales_API SHALL incluir un test de integración para el flujo de devolución parcial: completar venta → devolver 2 de 5 items → verificar stock parcialmente restaurado.
7. THE Sales_API SHALL incluir un test de integración para el flujo de cancelación: crear venta → agregar items → cancelar → verificar que no se puede modificar.
8. THE Sales_API SHALL alcanzar una cobertura mínima del 80% de líneas totales del proyecto, medida con JaCoCo.

---

### Requirement 20: Manejo de Errores Global

**User Story:** Como desarrollador del frontend, quiero que todos los errores del backend tengan un formato JSON consistente, para que pueda manejarlos de forma uniforme en la UI.

#### Acceptance Criteria

1. THE Sales_API SHALL implementar un manejador global de excepciones (`@ControllerAdvice`) que capture todas las excepciones no controladas y retorne respuestas JSON con el formato `{"message": "<descripción>", "timestamp": "<ISO-8601>", "path": "<endpoint>"}`.
2. WHEN Jakarta Bean Validation detecta errores de validación, THE Sales_API SHALL retornar HTTP 400 con un cuerpo JSON que liste todos los campos inválidos y sus mensajes de error.
3. THE Sales_API SHALL retornar HTTP 404 con mensaje descriptivo cuando se solicita una Sale, Receipt o recurso que no existe.
4. THE Sales_API SHALL retornar HTTP 405 cuando se usa un método HTTP no permitido en un endpoint.
5. THE Sales_API SHALL registrar en los logs todos los errores HTTP 5xx con nivel ERROR, incluyendo el stack trace completo.
