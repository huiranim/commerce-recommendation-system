package com.commerce.api.controller;

import com.commerce.api.dto.ProductRequest;
import com.commerce.api.dto.ProductResponse;
import com.commerce.api.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest req) {
        return ProductResponse.from(productService.create(req));
    }

    @GetMapping("/{productId}")
    public ProductResponse findById(@PathVariable String productId) {
        return ProductResponse.from(productService.findById(productId));
    }
}
