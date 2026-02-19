package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Product name must not be blank")
    private String name;

    @NotNull(message = "Product price must not be null")
    @Min(value = 0, message = "Product price must be zero or positive")
    private Double price;

    @NotNull(message = "Product quantity must not be null")
    @Min(value = 0, message = "Product quantity must be zero or positive")
    private Integer quantity;
}

