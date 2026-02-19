package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for communicating with PRODUCT-SERVICE.
 * Service name is resolved by Eureka - no hardcoded host/port needed.
 */
@FeignClient(
        name = "PRODUCT-SERVICE",
        fallback = ProductClientFallback.class
)
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductResponse getProductById(@PathVariable("id") Long id);

    @PutMapping("/products/reduce/{id}")
    ProductResponse reduceProductQuantity(@PathVariable("id") Long id,
                                          @RequestParam("quantity") int quantity);
}

