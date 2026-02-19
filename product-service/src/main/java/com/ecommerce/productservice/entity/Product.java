package com.ecommerce.productservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name must not be blank")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Product price must not be null")
    @Min(value = 0, message = "Product price must be zero or positive")
    @Column(nullable = false)
    private Double price;

    @NotNull(message = "Product quantity must not be null")
    @Min(value = 0, message = "Product quantity must be zero or positive")
    @Column(nullable = false)
    private Integer quantity;
}

