package com.commerce.api.service;

import com.commerce.api.domain.Category;
import com.commerce.api.domain.Product;
import com.commerce.api.dto.ProductRequest;
import com.commerce.api.exception.BusinessException;
import com.commerce.api.repository.CategoryRepository;
import com.commerce.api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @InjectMocks ProductService productService;

    @Test
    void create_성공() {
        var category = new Category("electronics", "전자제품");
        var req = new ProductRequest("노트북", "electronics", BigDecimal.valueOf(1200000));
        var product = new Product("노트북", category, BigDecimal.valueOf(1200000));

        when(categoryRepository.findById("electronics")).thenReturn(Optional.of(category));
        when(productRepository.save(any())).thenReturn(product);

        var result = productService.create(req);

        assertThat(result.getName()).isEqualTo("노트북");
        assertThat(result.getCategory().getId()).isEqualTo("electronics");
    }

    @Test
    void create_존재하지_않는_카테고리_예외() {
        var req = new ProductRequest("노트북", "unknown", BigDecimal.valueOf(1000));
        when(categoryRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("unknown");
    }

    @Test
    void findById_없는_상품_예외() {
        when(productRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById("bad-id"))
            .isInstanceOf(BusinessException.class);
    }
}
