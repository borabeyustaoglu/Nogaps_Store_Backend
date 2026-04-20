package org.example.common.config;

import lombok.RequiredArgsConstructor;
import org.example.common.entity.AppPermission;
import org.example.common.entity.AppRole;
import org.example.data.repository.AppPermissionRepository;
import org.example.data.repository.AppRoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final AppPermissionRepository permissionRepository;
    private final AppRoleRepository roleRepository;

    @Bean
    public CommandLineRunner seedRolePermissions() {
        return args -> {
            Map<String, String> definitions = new HashMap<>();
            definitions.put("CART_ADD", "Kullanici sepete urun ekleyebilir.");
            definitions.put("CART_REMOVE", "Kullanici sepetten urun cikarabilir.");
            definitions.put("CART_LIST", "Kullanici kendi sepetindeki urunleri listeleyebilir.");

            definitions.put("CHECKOUT_PREVIEW", "Kullanici odeme on izleme yapabilir.");
            definitions.put("ORDER_CREATE", "Kullanici sepetten siparis olusturabilir.");
            definitions.put("ORDER_PAY", "Kullanici siparis odemesi yapabilir.");
            definitions.put("ORDER_RETRY_PAYMENT", "Kullanici basarisiz siparis odemesini tekrar deneyebilir.");
            definitions.put("ORDER_LIST_SELF", "Kullanici kendi siparislerini listeleyebilir.");
            definitions.put("ORDER_DETAIL_SELF", "Kullanici kendi siparis detayini goruntuleyebilir.");

            definitions.put("COUPON_LIST_SELF", "Kullanici kendisine atanan kuponlari listeleyebilir.");
            definitions.put("COUPON_MANAGE", "Manager kupon olusturabilir ve kullanicilara atayabilir.");

            definitions.put("PRODUCT_CREATE", "Administrator yeni urun olusturabilir.");
            definitions.put("PRODUCT_UPDATE", "Administrator urun bilgilerini guncelleyebilir.");
            definitions.put("PRODUCT_DELETE", "Administrator urun silebilir.");

            definitions.put("CATEGORY_CREATE", "Manager yeni urun kategorisi olusturabilir.");
            definitions.put("CATEGORY_UPDATE", "Manager kategori bilgilerini guncelleyebilir.");
            definitions.put("CATEGORY_DELETE", "Manager kategori silebilir.");

            for (Map.Entry<String, String> item : definitions.entrySet()) {
                if (permissionRepository.findByCode(item.getKey()).isEmpty()) {
                    AppPermission permission = new AppPermission();
                    permission.setCode(item.getKey());
                    permission.setDescription(item.getValue());
                    permissionRepository.save(permission);
                }
            }

            createOrUpdateRole(
                    "USER",
                    Set.of(
                            "CART_ADD", "CART_REMOVE", "CART_LIST",
                            "CHECKOUT_PREVIEW", "ORDER_CREATE", "ORDER_PAY", "ORDER_RETRY_PAYMENT", "ORDER_LIST_SELF", "ORDER_DETAIL_SELF",
                            "COUPON_LIST_SELF"
                    )
            );
            createOrUpdateRole("ADMINISTRATOR", Set.of("PRODUCT_CREATE", "PRODUCT_UPDATE", "PRODUCT_DELETE"));
            createOrUpdateRole("MANAGER", Set.of("CATEGORY_CREATE", "CATEGORY_UPDATE", "CATEGORY_DELETE", "COUPON_MANAGE"));
        };
    }

    private void createOrUpdateRole(String roleName, Set<String> permissionCodes) {
        AppRole role = roleRepository.findByName(roleName).orElseGet(AppRole::new);
        role.setName(roleName);

        List<AppPermission> permissions = permissionCodes.stream()
                .map(code -> permissionRepository.findByCode(code)
                        .orElseThrow(() -> new IllegalStateException("Permission not found: " + code)))
                .toList();

        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);
        roleRepository.save(role);
    }
}