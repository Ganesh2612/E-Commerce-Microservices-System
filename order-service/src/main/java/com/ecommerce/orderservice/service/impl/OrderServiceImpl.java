package com.ecommerce.orderservice.service.impl;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.ProductResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.ProductServiceException;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final String PRODUCT_SERVICE = "productService";

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    @Override
    @CircuitBreaker(name = PRODUCT_SERVICE, fallbackMethod = "placeOrderFallback")
    @Retry(name = PRODUCT_SERVICE)
    public OrderResponse placeOrder(OrderRequest request) {
        log.info("Placing order for productId: {}, quantity: {}", request.getProductId(), request.getQuantity());

        // Step 1: Fetch product details from Product Service
        ProductResponse product = productClient.getProductById(request.getProductId());
        if (product == null) {
            throw new ProductServiceException(
                    "Product Service is unavailable. Please try again later.");
        }

        // Step 2: Reduce product quantity via Product Service
        ProductResponse updatedProduct = productClient.reduceProductQuantity(
                request.getProductId(), request.getQuantity());
        if (updatedProduct == null) {
            throw new ProductServiceException(
                    "Failed to reduce product stock. Please try again later.");
        }

        // Step 3: Calculate total amount
        double totalAmount = product.getPrice() * request.getQuantity();

        // Step 4: Save the order
        Order order = Order.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalAmount(totalAmount)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order placed successfully with id: {}", savedOrder.getId());

        return mapToOrderResponse(savedOrder, "SUCCESS");
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return mapToOrderResponse(order, "SUCCESS");
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders");

        return orderRepository.findAll()
                .stream()
                .map(order -> mapToOrderResponse(order, "SUCCESS"))
                .collect(Collectors.toList());
    }

    /**
     * Fallback method invoked when Product Service circuit breaker opens.
     */
    public OrderResponse placeOrderFallback(OrderRequest request, Throwable throwable) {
        log.error("Circuit breaker triggered for placeOrder. Cause: {}", throwable.getMessage());

        return OrderResponse.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalAmount(0.0)
                .status("FAILED - Product Service unavailable. Please try again later.")
                .build();
    }

    private OrderResponse mapToOrderResponse(Order order, String status) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(status)
                .build();
    }
}



