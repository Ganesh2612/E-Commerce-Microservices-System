package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.ProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for ProductClient.
 * Invoked when Product Service is unavailable (circuit breaker open / timeout).
 */
@Component
@Slf4j
public class ProductClientFallback implements ProductClient {

    @Override
    public ProductResponse getProductById(Long id) {
        log.warn("Fallback: Product Service unavailable. Cannot fetch product id: {}", id);
        return null;
    }

    @Override
    public ProductResponse reduceProductQuantity(Long id, int quantity) {
        log.warn("Fallback: Product Service unavailable. Cannot reduce quantity for product id: {}", id);
        return null;
    }
}

