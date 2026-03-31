package com.commerce.api.service;

import com.commerce.api.domain.Product;
import com.commerce.api.exception.BusinessException;
import com.commerce.api.exception.ErrorCode;
import com.commerce.api.repository.CategoryRepository;
import com.commerce.api.repository.ProductRepository;
import com.commerce.api.dto.ProductRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Product create(ProductRequest req) {
        var category = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, req.categoryId()));
        var saved = productRepository.save(new Product(req.name(), category, req.price()));
        // flush and re-fetch with category to avoid LazyInitializationException in DTO mapping
        productRepository.flush();
        return productRepository.findWithCategoryById(saved.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, saved.getId()));
    }

    public Product findById(String id) {
        return productRepository.findWithCategoryById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, id));
    }
}
