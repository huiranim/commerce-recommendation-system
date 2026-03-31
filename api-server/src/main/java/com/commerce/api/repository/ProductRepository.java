package com.commerce.api.repository;

import com.commerce.api.domain.Product;
import com.commerce.api.domain.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findAllByIdInAndStatus(List<String> ids, ProductStatus status);
}
