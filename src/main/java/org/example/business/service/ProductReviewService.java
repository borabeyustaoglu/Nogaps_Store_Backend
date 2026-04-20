package org.example.business.service;

import lombok.RequiredArgsConstructor;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.review.ProductReviewCreateRequest;
import org.example.common.dto.review.ProductReviewResponse;
import org.example.common.entity.AppUser;
import org.example.common.entity.Product;
import org.example.common.entity.ProductReview;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.common.security.SecurityUtils;
import org.example.data.repository.AppUserRepository;
import org.example.data.repository.ProductRepository;
import org.example.data.repository.ProductReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository appUserRepository;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ProductReviewResponse> listByProductId(Integer productId) {
        ensureProductExists(productId);
        return productReviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductReviewResponse create(Integer productId, ProductReviewCreateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Integer currentUserId = securityUtils.getCurrentUserId();
        AppUser user = appUserRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.getRating());
        review.setTitle(request.getTitle() == null ? "" : request.getTitle().trim());
        review.setComment(request.getComment().trim());

        ProductReview saved = productReviewRepository.save(review);
        auditLogService.log(
                "PRODUCT_REVIEW_CREATE",
                "PRODUCT",
                productId,
                "Review added for product: " + product.getName() + " by " + user.getUsername()
        );

        return toResponse(saved);
    }

    @Transactional
    public MessageResponse delete(Integer productId, Integer reviewId) {
        ProductReview review = productReviewRepository.findByIdAndProductId(reviewId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        Integer currentUserId = securityUtils.getCurrentUserId();
        AppUser user = appUserRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String roleName = user.getRole() != null ? user.getRole().getName() : "";
        boolean canDelete = "ADMINISTRATOR".equalsIgnoreCase(roleName) || "MANAGER".equalsIgnoreCase(roleName);
        if (!canDelete) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        productReviewRepository.delete(review);
        auditLogService.log(
                "PRODUCT_REVIEW_DELETE",
                "PRODUCT",
                productId,
                "Review deleted by " + user.getUsername() + " for product: " + review.getProduct().getName()
        );

        return new MessageResponse("Yorum silindi.");
    }

    private void ensureProductExists(Integer productId) {
        if (!productRepository.existsById(productId)) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    private ProductReviewResponse toResponse(ProductReview review) {
        return new ProductReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getUser().getId(),
                review.getUser().getUsername(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
