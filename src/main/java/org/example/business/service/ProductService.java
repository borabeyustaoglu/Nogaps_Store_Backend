package org.example.business.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.product.ProductListDto;
import org.example.common.dto.product.ProductRequest;
import org.example.common.entity.Product;
import org.example.common.entity.ProductCategory;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.common.mapper.ProductMapper;
import org.example.common.spec.ProductSpecDefinitions;
import org.example.data.repository.CartItemRepository;
import org.example.data.repository.OrderItemRepository;
import org.example.data.repository.ProductCategoryRepository;
import org.example.data.repository.ProductFavoriteRepository;
import org.example.data.repository.ProductRepository;
import org.example.data.repository.ProductReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductFavoriteRepository productFavoriteRepository;
    private final ProductReviewRepository productReviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ProductListDto> listAll() {
        return productRepository.findAllByOrderByIdAsc()
                .stream()
                .map(productMapper::toListDto)
                .toList();
    }

    @Transactional
    public MessageResponse create(ProductRequest request) {
        ProductCategory category = productCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setSpecsJson(serializeSpecs(ProductSpecDefinitions.sanitizeForCategory(category.getName(), request.getSpecs())));
        productRepository.save(product);
        auditLogService.log("PRODUCT_CREATE", "PRODUCT", product.getId(), "Product created: " + product.getName());

        return new MessageResponse("Urun olusturuldu.");
    }

    @Transactional
    public MessageResponse update(Integer productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        ProductCategory category = productCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setSpecsJson(serializeSpecs(ProductSpecDefinitions.sanitizeForCategory(category.getName(), request.getSpecs())));
        productRepository.save(product);
        auditLogService.log("PRODUCT_UPDATE", "PRODUCT", product.getId(), "Product updated: " + product.getName());

        return new MessageResponse("Urun guncellendi.");
    }

    @Transactional
    public MessageResponse delete(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (orderItemRepository.existsByProductId(productId)) {
            throw new AppException(ErrorCode.PRODUCT_HAS_ORDER_ITEMS);
        }

        cartItemRepository.deleteByProductId(productId);
        productFavoriteRepository.deleteByProductId(productId);
        productReviewRepository.deleteByProductId(productId);
        productRepository.delete(product);

        auditLogService.log("PRODUCT_DELETE", "PRODUCT", productId, "Product hard deleted: " + product.getName());
        return new MessageResponse("Urun silindi.");
    }

    private String serializeSpecs(Map<String, String> specs) {
        try {
            return objectMapper.writeValueAsString(specs == null ? Map.of() : specs);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }
}