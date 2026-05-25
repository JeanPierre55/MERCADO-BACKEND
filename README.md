# POS MERCATO Backend

REST API para gestión de transacciones de venta en terminales POS de un supermercado.

## Requisitos

- Java 17+
- Maven 3.9.5+

## Estructura del Proyecto

```
src/main/java/com/mercato/pos/
├── config/          # Configuración de Spring (Security, CORS, RestTemplate, OpenAPI)
├── controller/      # Controladores REST
├── service/         # Lógica de negocio
├── repository/      # Acceso a datos (Spring Data JPA)
├── model/           # Entidades JPA
├── dto/             # Data Transfer Objects
├── exception/       # Excepciones personalizadas y manejador global
├── scheduler/       # Tareas programadas
└── util/            # Utilidades (cálculos monetarios, etc.)
```

## Configuración

### application.yml

- **Puerto**: 8080
- **Base de datos**: H2 en memoria (jdbc:h2:mem:posdb)
- **JWT Secret**: pos-mercato-secret-key-256-bits-minimum (configurable via JWT_SECRET)
- **Product API**: http://localhost:8081 (configurable via PRODUCT_API_URL)
- **Customer API**: http://localhost:8082 (configurable via CUSTOMER_API_URL)
- **Tax Rate**: 19%
- **Frozen Sale Expiration**: 2 horas
- **Frozen Sale Check Interval**: 5 minutos

## Compilación y Ejecución

### Compilar

```bash
./mvnw clean compile
```

### Ejecutar tests

```bash
./mvnw test
```

### Ejecutar la aplicación

```bash
./mvnw spring-boot:run
```

### Generar reporte de cobertura

```bash
./mvnw verify
```

## Documentación de la API

Una vez que la aplicación esté en ejecución:

- **OpenAPI Docs**: http://localhost:8080/v3/api-docs
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console

## Dependencias Principales

- Spring Boot 3.2.0
- Spring Security (JWT con JJWT 0.11.5)
- Spring Data JPA
- H2 Database
- SpringDoc OpenAPI (Swagger UI)
- JUnit 5
- Mockito
- jqwik (Property-Based Testing)
- WireMock (Integration Testing)
- JaCoCo (Code Coverage)

## Cobertura de Código

- **Capa de servicios**: Mínimo 90%
- **Proyecto completo**: Mínimo 80%

