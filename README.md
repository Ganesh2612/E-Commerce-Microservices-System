# E-Commerce Microservices System

A distributed E-Commerce backend built with **Spring Boot**, **Spring Cloud**, **Netflix Eureka**, **Spring Cloud Gateway**, **OpenFeign**, and **Resilience4j**.

---

## Architecture

```
Client
  ↓
API Gateway (8080)          ← Single entry point for all clients
  ↓
Eureka Server (8761)        ← Service registry & discovery
  ↓
┌──────────────────┬──────────────────┐
│  Product Service │   Order Service  │
│     (8081)       │     (8082)       │
└──────────────────┴──────────────────┘
       ↓                    ↓
  Product DB (H2)       Order DB (H2)
```

---

## Modules

| Module           | Port | Description                          |
|------------------|------|--------------------------------------|
| `eureka-server`  | 8761 | Service registry (Netflix Eureka)    |
| `api-gateway`    | 8080 | API Gateway (Spring Cloud Gateway)   |
| `product-service`| 8081 | Product CRUD + stock management      |
| `order-service`  | 8082 | Order placement with Feign + Resilience4j |

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+

### Build all modules
```bash
mvn clean install -DskipTests
```

### Run services (in order)

1. **Eureka Server**
```bash
cd eureka-server && mvn spring-boot:run
```

2. **API Gateway**
```bash
cd api-gateway && mvn spring-boot:run
```

3. **Product Service**
```bash
cd product-service && mvn spring-boot:run
```

4. **Order Service**
```bash
cd order-service && mvn spring-boot:run
```

---

## API Reference (via API Gateway on port 8080)

### Product Service — `http://localhost:8080/products`

| Method   | Endpoint                           | Description                          |
|----------|------------------------------------|--------------------------------------|
| `POST`   | `/products`                        | Create a new product                 |
| `GET`    | `/products`                        | Get all products                     |
| `GET`    | `/products/{id}`                   | Get product by ID                    |
| `PUT`    | `/products/{id}`                   | Update a product                     |
| `DELETE` | `/products/{id}`                   | Delete a product                     |
| `PUT`    | `/products/reduce/{id}?quantity=N` | Reduce stock (internal - Order Svc)  |

**Create Product Request:**
```json
{
  "name": "Laptop",
  "price": 50000,
  "quantity": 10
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Laptop",
  "price": 50000,
  "quantity": 10
}
```

---

### Order Service — `http://localhost:8080/orders`

| Method | Endpoint       | Description         |
|--------|----------------|---------------------|
| `POST` | `/orders`      | Place a new order   |
| `GET`  | `/orders/{id}` | Get order by ID     |
| `GET`  | `/orders`      | Get all orders      |

**Place Order Request:**
```json
{
  "productId": 1,
  "quantity": 2
}
```

**Response:**
```json
{
  "orderId": 1,
  "productId": 1,
  "quantity": 2,
  "totalAmount": 100000,
  "status": "SUCCESS"
}
```

---

## Inter-Service Communication

Order Service calls Product Service using **OpenFeign** with Eureka-based service discovery:

```java
@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {
    @GetMapping("/products/{id}")
    ProductResponse getProductById(@PathVariable Long id);

    @PutMapping("/products/reduce/{id}")
    ProductResponse reduceProductQuantity(@PathVariable Long id, @RequestParam int quantity);
}
```

No hardcoded URLs — Eureka resolves `PRODUCT-SERVICE` to the actual host/port automatically.

---

## Fault Tolerance (Resilience4j)

Order Service is protected with:

| Feature          | Configuration                                      |
|------------------|----------------------------------------------------|
| Circuit Breaker  | Opens after 50% failure rate over 10 calls         |
| Retry            | 3 attempts with exponential backoff (1s base)      |
| Timeout          | 3 seconds per request                              |
| Fallback         | Returns `FAILED` status with 0 total amount        |

---

## Exception Handling

All services use `@RestControllerAdvice` for global exception handling.

**Error Response format:**
```json
{
  "timestamp": "2026-02-19T10:00:00",
  "message": "Product not found with id: 99",
  "status": 404,
  "error": "Not Found",
  "path": "/products/99"
}
```

**Custom Exceptions:**
- `ProductNotFoundException` → 404 Not Found
- `InsufficientStockException` → 400 Bad Request
- `OrderNotFoundException` → 404 Not Found
- `ProductServiceException` → 400 Bad Request

---

## H2 Console (Development)

| Service          | H2 Console URL                                   | JDBC URL                      |
|------------------|--------------------------------------------------|-------------------------------|
| Product Service  | http://localhost:8081/h2-console                 | `jdbc:h2:mem:productdb`       |
| Order Service    | http://localhost:8082/h2-console                 | `jdbc:h2:mem:orderdb`         |

---

## Project Structure

```
ecommerce-microservices/
├── pom.xml                          ← Parent POM (multi-module)
├── eureka-server/
│   ├── pom.xml
│   └── src/main/java/.../EurekaServerApplication.java
├── api-gateway/
│   ├── pom.xml
│   └── src/main/java/.../
│       ├── ApiGatewayApplication.java
│       └── controller/FallbackController.java
├── product-service/
│   ├── pom.xml
│   └── src/
│       ├── main/java/.../
│       │   ├── controller/ProductController.java
│       │   ├── service/ProductService.java
│       │   ├── service/impl/ProductServiceImpl.java
│       │   ├── repository/ProductRepository.java
│       │   ├── entity/Product.java
│       │   ├── dto/ProductRequest.java
│       │   ├── dto/ProductResponse.java
│       │   └── exception/
│       │       ├── GlobalExceptionHandler.java
│       │       ├── ProductNotFoundException.java
│       │       ├── InsufficientStockException.java
│       │       └── ErrorResponse.java
│       └── test/java/.../service/impl/ProductServiceImplTest.java
└── order-service/
    ├── pom.xml
    └── src/
        ├── main/java/.../
        │   ├── controller/OrderController.java
        │   ├── service/OrderService.java
        │   ├── service/impl/OrderServiceImpl.java
        │   ├── repository/OrderRepository.java
        │   ├── entity/Order.java
        │   ├── dto/OrderRequest.java
        │   ├── dto/OrderResponse.java
        │   ├── dto/ProductResponse.java
        │   ├── client/ProductClient.java
        │   ├── client/ProductClientFallback.java
        │   └── exception/
        │       ├── GlobalExceptionHandler.java
        │       ├── OrderNotFoundException.java
        │       ├── ProductServiceException.java
        │       └── ErrorResponse.java
        └── test/java/.../service/impl/OrderServiceImplTest.java
```

