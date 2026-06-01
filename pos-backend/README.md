# POS Serverless AWS

Backend serverless para un sistema de Punto de Venta (POS) básico, implementado con **AWS Lambda (Java 17)**, **API Gateway** y **DynamoDB**, desplegado con **AWS SAM**.

Proyecto académico que sigue la metodología **Spec-Driven Development (SDD)**.

---

## Arquitectura Serverless

```
Cliente (Postman / Frontend)
         │
         ▼
┌─────────────────────────────────────┐
│       AWS API Gateway (REST)        │
│                                     │
│  GET  /productos ──────────────┐    │
│  POST /ventas   ────────────┐  │    │
└────────────────────────────┼──┼────┘
                             │  │
              ┌──────────────┘  │
              │                 │
              ▼                 ▼
  ┌─────────────────┐  ┌─────────────────┐
  │  VentasFunction │  │ProductosFunction│
  │   (Java 17)     │  │   (Java 17)     │
  └────────┬────────┘  └────────┬────────┘
           │                    │
     ┌─────┴──────┐             │
     │            │             │
     ▼            ▼             ▼
┌─────────┐ ┌──────────┐ ┌──────────┐
│ Ventas  │ │Productos │ │Productos │
│  Table  │ │  Table   │ │  Table   │
│(DynamoDB│ │(DynamoDB)│ │(DynamoDB)│
└─────────┘ └──────────┘ └──────────┘
```

### Componentes

| Componente | Descripción |
|-----------|-------------|
| **API Gateway** | Punto de entrada HTTP, enruta requests a las Lambdas |
| **ProductosFunction** | Lambda Java 17 — consulta ProductosTable y retorna lista de productos |
| **VentasFunction** | Lambda Java 17 — valida request, verifica productos y registra venta |
| **ProductosTable** | Tabla DynamoDB con catálogo de productos (id, nombre, precio) |
| **VentasTable** | Tabla DynamoDB con registro de ventas (id, productos, timestamp) |

---

## Estructura del Repositorio

```
pos-backend/
│
├── .kiro/specs/pos-serverless-aws/   # Specs SDD (generados antes del código)
│   ├── requirements.md
│   ├── design.md
│   └── tasks.md
│
├── productos/                         # Lambda GET /productos
│   ├── src/
│   │   ├── main/java/com/pos/productos/
│   │   │   ├── ProductosHandler.java  # Handler principal
│   │   │   └── model/
│   │   │       └── Producto.java      # Modelo de datos
│   │   └── test/java/com/pos/productos/
│   │       └── ProductosHandlerTest.java
│   └── pom.xml
│
├── ventas/                            # Lambda POST /ventas
│   ├── src/
│   │   ├── main/java/com/pos/ventas/
│   │   │   ├── VentasHandler.java     # Handler principal
│   │   │   └── model/
│   │   │       ├── Venta.java
│   │   │       ├── VentaRequest.java
│   │   │       └── ProductoItem.java
│   │   └── test/java/com/pos/ventas/
│   │       └── VentasHandlerTest.java
│   └── pom.xml
│
├── template.yaml                      # Infraestructura AWS SAM
├── .gitignore
├── my-prompt.txt                      # Prompt SDD utilizado
└── README.md
```

---

## Prerrequisitos

Antes de construir y desplegar, asegúrate de tener instalado:

| Herramienta | Versión mínima | Instalación |
|------------|---------------|-------------|
| **Java JDK** | 17 | https://adoptium.net/ |
| **Maven** | 3.9+ | https://maven.apache.org/ |
| **AWS CLI** | 2.x | https://aws.amazon.com/cli/ |
| **AWS SAM CLI** | 1.x | https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html |
| **Cuenta AWS** | Configurada | `aws configure` |

Verificar instalación:
```bash
java -version
mvn -version
aws --version
sam --version
```

---

## Ejecutar Pruebas Unitarias

Las pruebas están completamente aisladas de AWS (usan mocks de Mockito):

```bash
# Pruebas del módulo productos
cd productos
mvn test

# Pruebas del módulo ventas
cd ../ventas
mvn test
```

Escenarios cubiertos:
- ✅ GET /productos — respuesta exitosa con productos
- ✅ GET /productos — tabla vacía retorna `[]`
- ✅ GET /productos — error de DynamoDB retorna HTTP 500
- ✅ POST /ventas — venta registrada correctamente (HTTP 201)
- ✅ POST /ventas — body inválido (HTTP 400)
- ✅ POST /ventas — array de productos vacío (HTTP 400)
- ✅ POST /ventas — producto inexistente (HTTP 404)
- ✅ POST /ventas — error de escritura en DynamoDB (HTTP 500)

---

## Build y Despliegue

### 1. Compilar y empaquetar

```bash
# Desde la raíz del proyecto pos-backend/
sam build
```

Este comando compila ambos módulos Maven y genera los uber JARs en `.aws-sam/build/`.

### 2. Desplegar en AWS

```bash
sam deploy --guided
```

Durante el proceso interactivo, ingresa:
- **Stack Name**: `pos-serverless-aws` (o el nombre que prefieras)
- **AWS Region**: `us-east-1` (o tu región preferida)
- **Confirm changes before deploy**: `Y`
- **Allow SAM CLI IAM role creation**: `Y`
- **Save arguments to configuration file**: `Y`

Al finalizar, SAM mostrará los **Outputs** con las URLs de los endpoints.

### 3. Despliegues posteriores (sin --guided)

```bash
sam deploy
```

---

## Insertar Datos de Prueba

Después del despliegue, inserta productos de ejemplo en DynamoDB:

```bash
# Obtener el nombre de la tabla (reemplaza STACK_NAME con tu stack)
STACK_NAME="pos-serverless-aws"
TABLE_NAME=$(aws cloudformation describe-stacks \
  --stack-name $STACK_NAME \
  --query "Stacks[0].Outputs[?OutputKey=='ProductosTableName'].OutputValue" \
  --output text)

# Insertar Laptop
aws dynamodb put-item \
  --table-name $TABLE_NAME \
  --item '{"id": {"S": "1"}, "nombre": {"S": "Laptop"}, "precio": {"N": "2500"}}'

# Insertar Mouse
aws dynamodb put-item \
  --table-name $TABLE_NAME \
  --item '{"id": {"S": "2"}, "nombre": {"S": "Mouse"}, "precio": {"N": "50"}}'

# Verificar los datos insertados
aws dynamodb scan --table-name $TABLE_NAME
```

---

## Probar con Postman

Obtén la URL base del API Gateway desde los Outputs del deploy:

```
https://{api-id}.execute-api.{region}.amazonaws.com/prod
```

### GET /productos

| Campo | Valor |
|-------|-------|
| Método | `GET` |
| URL | `{BASE_URL}/productos` |
| Headers | ninguno requerido |

**Respuesta exitosa (200):**
```json
[
  { "id": "1", "nombre": "Laptop", "precio": 2500.0 },
  { "id": "2", "nombre": "Mouse",  "precio": 50.0   }
]
```

**Tabla vacía (200):**
```json
[]
```

---

### POST /ventas

| Campo | Valor |
|-------|-------|
| Método | `POST` |
| URL | `{BASE_URL}/ventas` |
| Header | `Content-Type: application/json` |

**Body de ejemplo:**
```json
{
  "productos": [
    { "id": "1", "cantidad": 2 },
    { "id": "2", "cantidad": 1 }
  ]
}
```

**Respuesta exitosa (201):**
```json
{
  "message": "Venta registrada correctamente"
}
```

**Body inválido (400):**
```json
{
  "message": "Body inválido"
}
```

**Producto inexistente (404):**
```json
{
  "message": "Producto inexistente"
}
```

---

## Capturas Requeridas

Para la entrega académica, toma capturas de:

1. **GET /productos exitoso** — Postman mostrando HTTP 200 con array de productos
2. **POST /ventas exitoso** — Postman mostrando HTTP 201 con mensaje de éxito
3. **Caso de error** — Postman mostrando HTTP 400 o 404
4. **Pruebas unitarias exitosas** — Terminal mostrando `BUILD SUCCESS` de `mvn test`

---

## Proceso SDD (Spec-Driven Development)

Este proyecto siguió estrictamente la metodología **Spec-Driven Development**:

### ¿Qué es SDD?

SDD es un enfoque de desarrollo donde:
1. Se describe **qué** debe hacer el sistema en lenguaje natural
2. Se generan archivos de especificación estructurados (requirements, design, tasks)
3. Se revisan y refinan las especificaciones
4. Se implementa el código siguiendo las especificaciones

### Orden de creación en este proyecto

```
1. requirements.md  →  Qué debe hacer el sistema (12 requisitos)
2. design.md        →  Cómo está diseñado (arquitectura, contratos, pruebas)
3. tasks.md         →  Plan de implementación ordenado (18 tareas)
4. Código           →  Implementación guiada por los specs
```

### Beneficios observados

- Los contratos de API (request/response) estaban definidos antes de escribir una línea de código
- Las pruebas unitarias se diseñaron antes de implementar los handlers
- La estructura del proyecto fue decidida en el spec, no durante la implementación
- Los requisitos de IAM y seguridad se definieron en el diseño, no como afterthought

Los archivos de spec están en `.kiro/specs/pos-serverless-aws/`.

---

## Limpiar Recursos AWS

Para evitar costos, elimina el stack cuando termines:

```bash
sam delete --stack-name pos-serverless-aws
```

---

## Tecnologías Utilizadas

| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Java | 17 | Runtime de las Lambdas |
| AWS Lambda | — | Funciones serverless |
| AWS API Gateway | REST | Endpoints HTTP |
| AWS DynamoDB | — | Persistencia NoSQL |
| AWS SAM | 1.x | Infraestructura como código |
| Maven | 3.9+ | Build y gestión de dependencias |
| JUnit 5 | 5.10 | Framework de pruebas |
| Mockito | 5.6 | Mocks para aislar DynamoDB |
| Jackson | 2.15 | Serialización JSON |
| AWS SDK v2 | 2.21 | Cliente DynamoDB |
