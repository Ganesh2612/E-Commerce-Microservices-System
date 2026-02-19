package com.ecommerce.orderservice.service.impl;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.ProductResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.ProductServiceException;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderRequest orderRequest;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1L)
                .productId(1L)
                .quantity(2)
                .totalAmount(100000.0)
                .build();

        orderRequest = OrderRequest.builder()
                .productId(1L)
                .quantity(2)
                .build();

        productResponse = ProductResponse.builder()
                .id(1L)
                .name("Laptop")
                .price(50000.0)
                .quantity(10)
                .build();
    }

    @Test
    @DisplayName("Should place order successfully")
    void shouldPlaceOrderSuccessfully() {
        when(productClient.getProductById(1L)).thenReturn(productResponse);
        when(productClient.reduceProductQuantity(1L, 2)).thenReturn(productResponse);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.placeOrder(orderRequest);

        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getTotalAmount()).isEqualTo(100000.0);
        verify(productClient, times(1)).getProductById(1L);
        verify(productClient, times(1)).reduceProductQuantity(1L, 2);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw ProductServiceException when product not found via Feign")
    void shouldThrowProductServiceExceptionWhenProductNull() {
        when(productClient.getProductById(anyLong())).thenReturn(null);

        assertThatThrownBy(() -> orderService.placeOrder(orderRequest))
                .isInstanceOf(ProductServiceException.class)
                .hasMessageContaining("Product Service is unavailable");
    }

    @Test
    @DisplayName("Should throw ProductServiceException when reduce quantity returns null")
    void shouldThrowProductServiceExceptionWhenReduceQuantityNull() {
        when(productClient.getProductById(1L)).thenReturn(productResponse);
        when(productClient.reduceProductQuantity(anyLong(), anyInt())).thenReturn(null);

        assertThatThrownBy(() -> orderService.placeOrder(orderRequest))
                .isInstanceOf(ProductServiceException.class)
                .hasMessageContaining("Failed to reduce product stock");
    }

    @Test
    @DisplayName("Should return order by id")
    void shouldReturnOrderById() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1L);

        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getProductId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order not found")
    void shouldThrowOrderNotFoundExceptionWhenOrderNotFound() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should return all orders")
    void shouldReturnAllOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.getAllOrders();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getOrderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should return fallback response when circuit breaker triggers")
    void shouldReturnFallbackWhenCircuitBreakerTriggers() {
        RuntimeException ex = new RuntimeException("Service down");

        OrderResponse fallback = orderService.placeOrderFallback(orderRequest, ex);

        assertThat(fallback.getStatus()).contains("FAILED");
        assertThat(fallback.getTotalAmount()).isEqualTo(0.0);
    }
}

