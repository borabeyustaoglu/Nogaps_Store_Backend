package org.example.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.business.service.CouponService;
import org.example.business.service.PasswordPolicyService;
import org.example.common.entity.AppRole;
import org.example.common.entity.AppUser;
import org.example.common.entity.Cart;
import org.example.common.entity.CartItem;
import org.example.common.entity.Product;
import org.example.common.entity.ProductCategory;
import org.example.common.entity.ProductFavorite;
import org.example.common.entity.ProductReview;
import org.example.common.spec.ProductSpecDefinitions;
import org.example.data.repository.AppRoleRepository;
import org.example.data.repository.AppUserRepository;
import org.example.data.repository.CartItemRepository;
import org.example.data.repository.CartRepository;
import org.example.data.repository.ProductCategoryRepository;
import org.example.data.repository.ProductFavoriteRepository;
import org.example.data.repository.ProductRepository;
import org.example.data.repository.ProductReviewRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(2)
@RequiredArgsConstructor
public class SampleDataInitializer implements CommandLineRunner {
    private static final Set<String> TARGET_CATEGORY_NAMES = Set.of(
            "Mouse", "Klavye", "Kulaklik", "Ekran", "Kasa", "Kasa Icerikleri"
    );
    private static final Set<String> LEGACY_CATEGORY_NAMES = Set.of(
            "Electronics", "Home", "Sports", "Books"
    );

    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ProductFavoriteRepository productFavoriteRepository;
    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final ObjectMapper objectMapper;
    private final CouponService couponService;

    @Override
    public void run(String... args) {
        resetCatalogIfNeeded();
        seedCategories();
        seedProducts();
        backfillProductSpecs();
        seedUsers();
        couponService.backfillWelcomeCouponForAllUsers();
        seedProductReviews();
        seedFavorites();
        seedCartItems();
    }

    private void resetCatalogIfNeeded() {
        List<ProductCategory> existingCategories = categoryRepository.findAll();
        if (existingCategories.isEmpty() && productRepository.count() == 0) {
            return;
        }

        Set<String> existingCategoryNames = existingCategories.stream()
                .map(ProductCategory::getName)
                .collect(Collectors.toSet());

        boolean hasLegacyCategory = existingCategoryNames.stream().anyMatch(LEGACY_CATEGORY_NAMES::contains);
        boolean hasTargetCatalog = existingCategoryNames.containsAll(TARGET_CATEGORY_NAMES);

        if (hasLegacyCategory || !hasTargetCatalog) {
            cartItemRepository.deleteAll();
            productFavoriteRepository.deleteAll();
            productReviewRepository.deleteAll();
            productRepository.deleteAll();
            categoryRepository.deleteAll();
        }
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }

        ProductCategory mouse = new ProductCategory();
        mouse.setName("Mouse");
        mouse.setDescription("Oyuncu ve ofis kullanimina uygun mouse urunleri");

        ProductCategory keyboard = new ProductCategory();
        keyboard.setName("Klavye");
        keyboard.setDescription("Mekanik ve membran klavye modelleri");

        ProductCategory headset = new ProductCategory();
        headset.setName("Kulaklik");
        headset.setDescription("Oyun ve muzik icin kulaklik secenekleri");

        ProductCategory monitor = new ProductCategory();
        monitor.setName("Ekran");
        monitor.setDescription("Farkli boyut ve tazeleme hizlarinda monitorler");

        ProductCategory caseCategory = new ProductCategory();
        caseCategory.setName("Kasa");
        caseCategory.setDescription("Masaustu bilgisayar kasalari");

        ProductCategory components = new ProductCategory();
        components.setName("Kasa Icerikleri");
        components.setDescription("Kasa icin donanim bilesenleri");

        categoryRepository.saveAll(List.of(mouse, keyboard, headset, monitor, caseCategory, components));
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        List<ProductCategory> categories = categoryRepository.findAll();
        if (categories.size() < 6) {
            return;
        }

        Map<String, ProductCategory> categoryMap = new HashMap<>();
        for (ProductCategory category : categories) {
            categoryMap.put(category.getName(), category);
        }

        if (!categoryMap.keySet().containsAll(TARGET_CATEGORY_NAMES)) {
            return;
        }

        productRepository.saveAll(List.of(
                buildProduct("Logitech G Pro X Superlight", "Kablosuz, hafif oyuncu mouse", "3799.00", 35, categoryMap.get("Mouse")),
                buildProduct("Razer DeathAdder V3", "Yuksek hassasiyetli ergonomik oyuncu mouse", "3399.00", 30, categoryMap.get("Mouse")),
                buildProduct("SteelSeries Rival 5", "Programlanabilir tuslara sahip kablolu mouse", "2199.00", 26, categoryMap.get("Mouse")),
                buildProduct("ASUS TUF M4 Air", "Delikli govdeye sahip ultra hafif mouse", "1799.00", 33, categoryMap.get("Mouse")),

                buildProduct("SteelSeries Apex 9 TKL", "Mekanik, RGB oyuncu klavyesi", "4299.00", 28, categoryMap.get("Klavye")),
                buildProduct("Logitech G413 SE", "Dayanikli govdeye sahip mekanik klavye", "2499.00", 34, categoryMap.get("Klavye")),
                buildProduct("Razer BlackWidow V4 X", "Coklu ortam tuslari ile oyuncu klavyesi", "4699.00", 22, categoryMap.get("Klavye")),
                buildProduct("Keychron K8 Pro", "Kablosuz, hot-swap destekli mekanik klavye", "3599.00", 19, categoryMap.get("Klavye")),

                buildProduct("HyperX Cloud III", "7.1 surround destekli oyuncu kulakligi", "3299.00", 42, categoryMap.get("Kulaklik")),
                buildProduct("SteelSeries Arctis Nova 1", "Hafif tasarimli oyuncu kulakligi", "1999.00", 38, categoryMap.get("Kulaklik")),
                buildProduct("Logitech G435 Lightspeed", "Kablosuz ve dusuk gecikmeli kulaklik", "2899.00", 27, categoryMap.get("Kulaklik")),
                buildProduct("Corsair HS80 RGB", "Yuksek kaliteli mikrofonlu RGB kulaklik", "3699.00", 21, categoryMap.get("Kulaklik")),

                buildProduct("LG UltraGear 27GN800-B", "27 inc, 144Hz, QHD oyuncu monitoru", "8999.00", 17, categoryMap.get("Ekran")),
                buildProduct("Samsung Odyssey G5 27", "1000R kavislige sahip 165Hz monitor", "9699.00", 14, categoryMap.get("Ekran")),
                buildProduct("ASUS TUF VG249Q1A", "24 inc, IPS panel, 165Hz monitor", "6299.00", 24, categoryMap.get("Ekran")),
                buildProduct("AOC 24G2SPU", "1ms tepkime suresi sunan FHD oyuncu monitoru", "5999.00", 20, categoryMap.get("Ekran")),

                buildProduct("NZXT H5 Flow", "Yuksek hava akisina sahip mid tower kasa", "4599.00", 14, categoryMap.get("Kasa")),
                buildProduct("Corsair 4000D Airflow", "Genis ic hacimli ve mesh on panelli kasa", "4999.00", 16, categoryMap.get("Kasa")),
                buildProduct("Lian Li Lancool 216", "Cift buyuk fanli performans odakli kasa", "5399.00", 12, categoryMap.get("Kasa")),
                buildProduct("Cooler Master TD500 Mesh V2", "Ustun sogutma ve RGB destekli kasa", "5199.00", 13, categoryMap.get("Kasa")),

                buildProduct("Corsair RM850e 850W", "80+ Gold, tam moduler guc kaynagi", "3899.00", 25, categoryMap.get("Kasa Icerikleri")),
                buildProduct("MSI B650 Tomahawk WiFi", "AM5 soket destekli anakart", "7299.00", 15, categoryMap.get("Kasa Icerikleri")),
                buildProduct("Kingston Fury Beast 32GB DDR5", "Yuksek frekansli cift kanal RAM kiti", "4099.00", 29, categoryMap.get("Kasa Icerikleri")),
                buildProduct("Samsung 990 EVO 1TB NVMe SSD", "Yuksek hizli PCIe depolama birimi", "3199.00", 31, categoryMap.get("Kasa Icerikleri"))
        ));
    }

    private void backfillProductSpecs() {
        List<Product> products = productRepository.findAllByOrderByIdAsc();
        if (products.isEmpty()) {
            return;
        }

        boolean changed = false;
        int index = 0;
        for (Product product : products) {
            String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
            if (categoryName == null || ProductSpecDefinitions.fieldsForCategory(categoryName).isEmpty()) {
                index++;
                continue;
            }
            if (product.getSpecsJson() != null && !product.getSpecsJson().isBlank()) {
                index++;
                continue;
            }

            Map<String, String> specs = ProductSpecDefinitions.sampleSpecsForCategory(categoryName, index);
            product.setSpecsJson(serializeSpecs(specs));
            changed = true;
            index++;
        }

        if (changed) {
            productRepository.saveAll(products);
        }
    }

    private Product buildProduct(String name,
                                 String description,
                                 String price,
                                 Integer stockQuantity,
                                 ProductCategory category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stockQuantity);
        product.setCategory(category);
        return product;
    }

    private String serializeSpecs(Map<String, String> specs) {
        try {
            return objectMapper.writeValueAsString(specs);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void seedUsers() {
        AppRole userRole = appRoleRepository.findByName("USER").orElse(null);
        AppRole adminRole = appRoleRepository.findByName("ADMINISTRATOR").orElse(null);
        AppRole managerRole = appRoleRepository.findByName("MANAGER").orElse(null);

        if (userRole == null || adminRole == null || managerRole == null) {
            return;
        }

        upsertDemoUser(
                "demo.user",
                "User@1234",
                "Demo User",
                "user@example.com",
                "+90 555 100 00 01",
                "Istanbul, Turkey",
                userRole
        );

        upsertDemoUser(
                "demo.admin",
                "Admin@1234",
                "Demo Administrator",
                "admin@example.com",
                "+90 555 100 00 02",
                "Ankara, Turkey",
                adminRole
        );

        upsertDemoUser(
                "demo.manager",
                "Manager@1234",
                "Demo Manager",
                "manager@example.com",
                "+90 555 100 00 03",
                "Izmir, Turkey",
                managerRole
        );
    }

    private void seedProductReviews() {
        if (productReviewRepository.count() > 0) {
            return;
        }

        AppUser demoUser = appUserRepository.findByUsername("demo.user").orElse(null);
        AppUser demoManager = appUserRepository.findByUsername("demo.manager").orElse(null);
        AppUser demoAdmin = appUserRepository.findByUsername("demo.admin").orElse(null);

        if (demoUser == null || demoManager == null || demoAdmin == null) {
            return;
        }

        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return;
        }

        Product first = products.get(0);
        Product second = products.size() > 1 ? products.get(1) : first;
        Product third = products.size() > 2 ? products.get(2) : first;

        productReviewRepository.saveAll(List.of(
                buildReview(first, demoUser, 5, "Harika performans", "Cok hizli ve sessiz calisiyor, kesinlikle tavsiye ederim."),
                buildReview(second, demoManager, 4, "Fiyat performans", "Urun gayet iyi, teslimat hizliydi."),
                buildReview(third, demoAdmin, 5, "Kaliteli urun", "Malzeme kalitesi yuksek, beklentiyi karsiladi.")
        ));
    }

    private void seedFavorites() {
        if (productFavoriteRepository.count() > 0) {
            return;
        }

        AppUser demoUser = appUserRepository.findByUsername("demo.user").orElse(null);
        if (demoUser == null) {
            return;
        }

        List<Product> products = productRepository.findAll();
        if (products.size() < 3) {
            return;
        }

        productFavoriteRepository.saveAll(List.of(
                buildFavorite(demoUser, products.get(0)),
                buildFavorite(demoUser, products.get(5)),
                buildFavorite(demoUser, products.get(10))
        ));
    }

    private ProductReview buildReview(Product product,
                                      AppUser user,
                                      Integer rating,
                                      String title,
                                      String comment) {
        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(rating);
        review.setTitle(title);
        review.setComment(comment);
        return review;
    }

    private ProductFavorite buildFavorite(AppUser user, Product product) {
        ProductFavorite favorite = new ProductFavorite();
        favorite.setUser(user);
        favorite.setProduct(product);
        return favorite;
    }

    private void upsertDemoUser(String username,
                                String rawPassword,
                                String fullName,
                                String email,
                                String phoneNumber,
                                String address,
                                AppRole role) {
        AppUser user = appUserRepository.findByUsername(username).orElseGet(AppUser::new);
        user.setUsername(username);
        passwordPolicyService.validateOrThrow(rawPassword);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setAddress(address);
        user.setRole(role);
        appUserRepository.save(user);
    }

    private void seedCartItems() {
        AppUser user = appUserRepository.findByUsername("demo.user").orElse(null);
        List<Product> products = productRepository.findAll();
        if (user == null || products.size() < 3) {
            return;
        }
        Cart cart = cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });

        List<CartItem> existingItems = cartItemRepository.findByCartUserId(user.getId());
        if (!existingItems.isEmpty()) {
            cartItemRepository.deleteAll(existingItems);
        }

        CartItem item1 = new CartItem();
        item1.setCart(cart);
        item1.setUser(user);
        item1.setProduct(products.get(0));
        item1.setQuantity(2);

        CartItem item2 = new CartItem();
        item2.setCart(cart);
        item2.setUser(user);
        item2.setProduct(products.get(1));
        item2.setQuantity(1);

        CartItem item3 = new CartItem();
        item3.setCart(cart);
        item3.setUser(user);
        item3.setProduct(products.get(2));
        item3.setQuantity(3);

        cartItemRepository.saveAll(List.of(item1, item2, item3));
    }
}
