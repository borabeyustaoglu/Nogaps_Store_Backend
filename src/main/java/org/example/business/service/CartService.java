package org.example.business.service;

import lombok.RequiredArgsConstructor;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.cart.CartItemRequest;
import org.example.common.dto.cart.CartItemResponse;
import org.example.common.entity.AppUser;
import org.example.common.entity.Cart;
import org.example.common.entity.CartItem;
import org.example.common.entity.Product;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.data.repository.AppUserRepository;
import org.example.data.repository.CartRepository;
import org.example.data.repository.CartItemRepository;
import org.example.data.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public MessageResponse addToCart(Integer userId, CartItemRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        CartItem item = cartItemRepository.findByCartUserIdAndProductId(userId, request.getProductId())
                .orElseGet(CartItem::new);

        item.setCart(cart);
        item.setUser(user);
        item.setProduct(product);
        int currentQty = item.getQuantity() == null ? 0 : item.getQuantity();
        int newQuantity = currentQty + request.getQuantity();
        if (newQuantity > product.getStockQuantity()) {
            throw new AppException(ErrorCode.NOT_ENOUGH_STOCK, product.getName());
        }
        item.setQuantity(newQuantity);

        cartItemRepository.save(item);
        auditLogService.log(
                "CART_ADD",
                "CART",
                cart.getId(),
                "Added product " + product.getName() + ", quantity=" + request.getQuantity()
        );
        return new MessageResponse("Ürün sepete eklendi.");
    }

    @Transactional
    public MessageResponse removeFromCart(Integer userId, Integer productId) {
        CartItem item = cartItemRepository.findByCartUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_IN_CART));
        Integer cartId = item.getCart().getId();
        String productName = item.getProduct().getName();
        cartItemRepository.delete(item);
        auditLogService.log(
                "CART_REMOVE",
                "CART",
                cartId,
                "Removed product " + productName + " from cart"
        );
        return new MessageResponse("Ürün sepetten çıkarıldı.");
    }

    @Transactional(readOnly = true)
    public List<CartItemResponse> listCart(Integer userId) {
        return cartItemRepository.findByCartUserId(userId).stream()
                .map(item -> new CartItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getProduct().getPrice(),
                        item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();
    }

    private Cart getOrCreateCart(AppUser user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }
}
