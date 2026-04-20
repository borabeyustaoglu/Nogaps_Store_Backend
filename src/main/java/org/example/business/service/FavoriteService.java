package org.example.business.service;

import lombok.RequiredArgsConstructor;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.favorite.FavoriteProductResponse;
import org.example.common.entity.AppUser;
import org.example.common.entity.Product;
import org.example.common.entity.ProductCategory;
import org.example.common.entity.ProductFavorite;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.data.repository.AppUserRepository;
import org.example.data.repository.ProductFavoriteRepository;
import org.example.data.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final ProductFavoriteRepository productFavoriteRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<FavoriteProductResponse> listFavorites(Integer userId) {
        return productFavoriteRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MessageResponse addFavorite(Integer userId, Integer productId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        boolean exists = productFavoriteRepository.findByUserIdAndProductId(userId, productId).isPresent();
        if (exists) {
            return new MessageResponse("Urun zaten favorilerde.");
        }

        ProductFavorite favorite = new ProductFavorite();
        favorite.setUser(user);
        favorite.setProduct(product);
        productFavoriteRepository.save(favorite);

        auditLogService.log("FAVORITE_ADD", "PRODUCT", productId, "Product favorited: " + product.getName());
        return new MessageResponse("Urun favorilere eklendi.");
    }

    @Transactional
    public MessageResponse removeFavorite(Integer userId, Integer productId) {
        ProductFavorite favorite = productFavoriteRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        String productName = favorite.getProduct().getName();
        productFavoriteRepository.delete(favorite);
        auditLogService.log("FAVORITE_REMOVE", "PRODUCT", productId, "Product removed from favorites: " + productName);
        return new MessageResponse("Urun favorilerden kaldirildi.");
    }

    private FavoriteProductResponse toResponse(ProductFavorite favorite) {
        Product product = favorite.getProduct();
        ProductCategory category = product.getCategory();

        return new FavoriteProductResponse(
                favorite.getId(),
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null
        );
    }
}