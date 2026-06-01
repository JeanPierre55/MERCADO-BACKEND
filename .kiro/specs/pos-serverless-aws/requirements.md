# Requirements Document

## Introduction

This document specifies the requirements for a serverless backend system for a basic Point of Sale (POS) application designed for academic evaluation. The system provides two core functionalities: listing products and registering sales. The implementation uses AWS serverless technologies (API Gateway, Lambda, DynamoDB) and follows Spec-Driven Development (SDD) methodology.

The system is designed as a learning project to demonstrate serverless architecture patterns, AWS SAM infrastructure-as-code, Java Lambda development, and comprehensive unit testing with mocked dependencies.

## Glossary

- **API_Gateway**: AWS API Gateway service that provides HTTP endpoints for the POS system
- **Lambda_Function**: AWS Lambda serverless compute function that executes business logic
- **DynamoDB_Table**: AWS DynamoDB NoSQL database table for data persistence
- **Productos_Lambda**: Lambda function that handles product listing requests
- **Ventas_Lambda**: Lambda function that handles sales registration requests
- **Productos_Table**: DynamoDB table storing product information (id, nombre, precio)
- **Ventas_Table**: DynamoDB table storing sales transactions
- **SAM_Template**: AWS SAM (Serverless Application Model) template.yaml file defining infrastructure
- **Handler**: Java class implementing the Lambda function request handler interface
- **Mock**: Test double that simulates DynamoDB behavior in unit tests
- **IAM_Role**: AWS Identity and Access Management role granting Lambda permissions to access DynamoDB

## Requirements

### Requirement 1: Product Listing Endpoint

**User Story:** As a POS client application, I want to retrieve a list of all available products, so that I can display them to the cashier for sale selection.

#### Acceptance Criteria

1. WHEN a GET request is sent to /productos, THE API_Gateway SHALL route the request to Productos_Lambda
2. WHEN Productos_Lambda receives a valid request, THE Productos_Lambda SHALL query Productos_Table for all product records
3. WHEN Productos_Table contains product records, THE Productos_Lambda SHALL return HTTP 200 with a JSON array containing all products
4. THE response JSON SHALL include for each product: id (string), nombre (string), and precio (number)
5. WHEN Productos_Table is empty, THE Productos_Lambda SHALL return HTTP 200 with an empty JSON array
6. WHEN DynamoDB_Table query fails due to connection error, THE Productos_Lambda SHALL return HTTP 500 with a descriptive error message in JSON format
7. THE error response JSON SHALL include a "message" field describing the error

### Requirement 2: Sales Registration Endpoint

**User Story:** As a POS client application, I want to register completed sales transactions, so that I can maintain a record of all sales for business reporting.

#### Acceptance Criteria

1. WHEN a POST request is sent to /ventas, THE API_Gateway SHALL route the request to Ventas_Lambda
2. WHEN Ventas_Lambda receives a request, THE Ventas_Lambda SHALL validate that the request body is valid JSON
3. THE request body SHALL contain a "productos" field with an array of product items
4. THE each product item SHALL contain "id" (string) and "cantidad" (integer) fields
5. WHEN the request body is invalid or missing required fields, THE Ventas_Lambda SHALL return HTTP 400 with error message "Body inválido"
6. WHEN the request body is valid, THE Ventas_Lambda SHALL verify each product id exists in Productos_Table
7. WHEN a product id does not exist in Productos_Table, THE Ventas_Lambda SHALL return HTTP 404 with error message "Producto inexistente"
8. WHEN all products exist, THE Ventas_Lambda SHALL generate a unique sale id
9. THE Ventas_Lambda SHALL store the sale record in Ventas_Table with: id, productos array, and timestamp
10. WHEN the sale is successfully stored, THE Ventas_Lambda SHALL return HTTP 201 with JSON message "Venta registrada correctamente"
11. WHEN DynamoDB_Table write operation fails, THE Ventas_Lambda SHALL return HTTP 500 with a descriptive error message

### Requirement 3: AWS SAM Infrastructure Definition

**User Story:** As a developer, I want infrastructure defined as code using AWS SAM, so that I can deploy the entire serverless application with a single command.

#### Acceptance Criteria

1. THE SAM_Template SHALL define an API Gateway REST API resource
2. THE API_Gateway SHALL expose a GET method at path /productos
3. THE API_Gateway SHALL expose a POST method at path /ventas
4. THE SAM_Template SHALL define a Lambda function resource named ProductosFunction
5. THE ProductosFunction SHALL use Java 17 runtime
6. THE ProductosFunction SHALL have an HTTP GET event trigger for /productos path
7. THE SAM_Template SHALL define a Lambda function resource named VentasFunction
8. THE VentasFunction SHALL use Java 17 runtime
9. THE VentasFunction SHALL have an HTTP POST event trigger for /ventas path
10. THE SAM_Template SHALL define a DynamoDB table resource named ProductosTable
11. THE ProductosTable SHALL have a primary key attribute named "id" of type String
12. THE SAM_Template SHALL define a DynamoDB table resource named VentasTable
13. THE VentasTable SHALL have a primary key attribute named "id" of type String
14. THE SAM_Template SHALL define IAM_Role policies granting ProductosFunction read permissions to ProductosTable
15. THE SAM_Template SHALL define IAM_Role policies granting VentasFunction read permissions to ProductosTable
16. THE SAM_Template SHALL define IAM_Role policies granting VentasFunction write permissions to VentasTable
17. THE ProductosFunction SHALL receive the ProductosTable name as an environment variable
18. THE VentasFunction SHALL receive both ProductosTable and VentasTable names as environment variables

### Requirement 4: Lambda Function Implementation in Java

**User Story:** As a developer, I want Lambda functions implemented in Java 17, so that I can leverage strong typing and enterprise-grade tooling for the academic project.

#### Acceptance Criteria

1. THE Productos_Lambda SHALL be implemented in a Java class named ProductosHandler
2. THE ProductosHandler SHALL implement the AWS Lambda RequestHandler interface
3. THE ProductosHandler SHALL accept APIGatewayProxyRequestEvent as input
4. THE ProductosHandler SHALL return APIGatewayProxyResponseEvent as output
5. THE Ventas_Lambda SHALL be implemented in a Java class named VentasHandler
6. THE VentasHandler SHALL implement the AWS Lambda RequestHandler interface
7. THE VentasHandler SHALL accept APIGatewayProxyRequestEvent as input
8. THE VentasHandler SHALL return APIGatewayProxyResponseEvent as output
9. THE ProductosHandler SHALL use AWS SDK for Java v2 DynamoDB client
10. THE VentasHandler SHALL use AWS SDK for Java v2 DynamoDB client
11. THE each Lambda function SHALL initialize the DynamoDB client in the constructor for connection reuse
12. THE each Lambda function SHALL read table names from environment variables

### Requirement 5: Data Models

**User Story:** As a developer, I want well-defined Java model classes, so that I can work with type-safe data structures throughout the application.

#### Acceptance Criteria

1. THE system SHALL define a Producto Java class in the model package
2. THE Producto class SHALL have fields: id (String), nombre (String), precio (Double)
3. THE Producto class SHALL provide getter and setter methods for all fields
4. THE system SHALL define a Venta Java class in the model package
5. THE Venta class SHALL have fields: id (String), productos (List of product items), timestamp (String)
6. THE Venta class SHALL provide getter and setter methods for all fields
7. THE each model class SHALL be serializable to and from JSON using Jackson or Gson

### Requirement 6: Unit Testing with Mocked Dependencies

**User Story:** As a developer, I want comprehensive unit tests with mocked DynamoDB dependencies, so that I can verify business logic without requiring actual AWS infrastructure.

#### Acceptance Criteria

1. THE ProductosHandler SHALL have a unit test class named ProductosHandlerTest
2. THE ProductosHandlerTest SHALL use JUnit 5 framework
3. THE ProductosHandlerTest SHALL use Mockito to mock DynamoDB client
4. THE ProductosHandlerTest SHALL test successful product retrieval scenario
5. THE ProductosHandlerTest SHALL test empty table scenario returning empty array
6. THE ProductosHandlerTest SHALL test DynamoDB connection error scenario returning HTTP 500
7. THE VentasHandler SHALL have a unit test class named VentasHandlerTest
8. THE VentasHandlerTest SHALL use JUnit 5 framework
9. THE VentasHandlerTest SHALL use Mockito to mock DynamoDB client
10. THE VentasHandlerTest SHALL test successful sale registration scenario
11. THE VentasHandlerTest SHALL test invalid request body scenario returning HTTP 400
12. THE VentasHandlerTest SHALL test non-existent product scenario returning HTTP 404
13. THE VentasHandlerTest SHALL test DynamoDB write error scenario returning HTTP 500
14. THE each test SHALL verify correct HTTP status codes
15. THE each test SHALL verify correct response body structure and content

### Requirement 7: Maven Build Configuration

**User Story:** As a developer, I want Maven configured for each Lambda function, so that I can build, test, and package the functions independently.

#### Acceptance Criteria

1. THE productos directory SHALL contain a pom.xml file
2. THE productos pom.xml SHALL declare Java 17 as source and target version
3. THE productos pom.xml SHALL include AWS Lambda Java Core dependency
4. THE productos pom.xml SHALL include AWS Lambda Java Events dependency
5. THE productos pom.xml SHALL include AWS SDK for Java v2 DynamoDB dependency
6. THE productos pom.xml SHALL include JUnit 5 dependency with test scope
7. THE productos pom.xml SHALL include Mockito dependency with test scope
8. THE productos pom.xml SHALL configure maven-shade-plugin to create an uber JAR
9. THE ventas directory SHALL contain a pom.xml file
10. THE ventas pom.xml SHALL declare Java 17 as source and target version
11. THE ventas pom.xml SHALL include all dependencies specified for productos pom.xml
12. THE ventas pom.xml SHALL configure maven-shade-plugin to create an uber JAR
13. WHEN mvn test is executed, THE Maven SHALL run all unit tests and report results
14. WHEN mvn package is executed, THE Maven SHALL create a deployable JAR file

### Requirement 8: Project Structure and Organization

**User Story:** As a developer, I want a well-organized project structure, so that I can easily navigate and maintain the codebase.

#### Acceptance Criteria

1. THE project root SHALL contain a template.yaml file for SAM infrastructure
2. THE project root SHALL contain a README.md file with project documentation
3. THE project root SHALL contain a .gitignore file
4. THE project SHALL have a productos directory for the products Lambda function
5. THE productos directory SHALL have src/main/java/com/pos/productos structure
6. THE productos directory SHALL have src/test/java/com/pos/productos structure
7. THE productos directory SHALL contain ProductosHandler.java in the main source path
8. THE productos directory SHALL contain ProductosHandlerTest.java in the test source path
9. THE productos directory SHALL have a model subdirectory containing Producto.java
10. THE project SHALL have a ventas directory for the sales Lambda function
11. THE ventas directory SHALL have src/main/java/com/pos/ventas structure
12. THE ventas directory SHALL have src/test/java/com/pos/ventas structure
13. THE ventas directory SHALL contain VentasHandler.java in the main source path
14. THE ventas directory SHALL contain VentasHandlerTest.java in the test source path
15. THE ventas directory SHALL have a model subdirectory containing Venta.java

### Requirement 9: Documentation and Deployment Instructions

**User Story:** As a student or evaluator, I want comprehensive documentation, so that I can understand, build, test, and deploy the system.

#### Acceptance Criteria

1. THE README.md SHALL include a project description section
2. THE README.md SHALL include a serverless architecture description or diagram
3. THE README.md SHALL include a repository structure section listing all directories and key files
4. THE README.md SHALL list all prerequisites: Java 17, Maven, AWS CLI, AWS SAM CLI, configured AWS account
5. THE README.md SHALL provide the command to build the project: sam build
6. THE README.md SHALL provide the command to deploy the project: sam deploy --guided
7. THE README.md SHALL explain how to test GET /productos endpoint using Postman
8. THE README.md SHALL provide an example request body for POST /ventas endpoint
9. THE README.md SHALL explain how to test POST /ventas endpoint using Postman
10. THE README.md SHALL provide the command to run unit tests: mvn test
11. THE README.md SHALL include a "Proceso SDD" section explaining how Spec-Driven Development was followed
12. THE README.md SHALL explain that specs were written before implementation

### Requirement 10: Security and Best Practices

**User Story:** As a developer, I want the project to follow security best practices, so that sensitive information is not exposed.

#### Acceptance Criteria

1. THE .gitignore file SHALL exclude AWS credentials files
2. THE .gitignore file SHALL exclude .env files
3. THE .gitignore file SHALL exclude target/ directories
4. THE .gitignore file SHALL exclude .aws-sam/ build directory
5. THE .gitignore file SHALL exclude IDE-specific files (.idea/, .vscode/, *.iml)
6. THE Lambda functions SHALL NOT hardcode AWS credentials
7. THE Lambda functions SHALL use IAM roles for AWS service access
8. THE SAM_Template SHALL define least-privilege IAM policies for each Lambda function
9. THE DynamoDB tables SHALL use on-demand billing mode to avoid unexpected costs in academic environment

### Requirement 11: Error Handling and Response Format

**User Story:** As a client application, I want consistent error response formats, so that I can handle errors predictably.

#### Acceptance Criteria

1. WHEN an error occurs, THE Lambda_Function SHALL return a JSON response with a "message" field
2. THE HTTP 400 error response SHALL have body: {"message": "Body inválido"}
3. THE HTTP 404 error response SHALL have body: {"message": "Producto inexistente"}
4. THE HTTP 500 error response SHALL have body: {"message": "Error de conexión con DynamoDB"} or similar descriptive message
5. THE HTTP 201 success response for POST /ventas SHALL have body: {"message": "Venta registrada correctamente"}
6. THE HTTP 200 success response for GET /productos SHALL have body: array of product objects
7. THE all responses SHALL include Content-Type: application/json header
8. THE all responses SHALL include appropriate CORS headers for browser access

### Requirement 12: Sample Data for Testing

**User Story:** As a developer, I want sample product data, so that I can test the system end-to-end after deployment.

#### Acceptance Criteria

1. THE README.md SHALL provide instructions to manually insert sample products into ProductosTable
2. THE sample data SHALL include at least 2 products with realistic values
3. THE sample product 1 SHALL have: id="1", nombre="Laptop", precio=2500
4. THE sample product 2 SHALL have: id="2", nombre="Mouse", precio=50
5. THE README.md SHALL provide AWS CLI commands or AWS Console instructions for data insertion

