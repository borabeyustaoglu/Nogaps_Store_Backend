package org.example.common.exception;

public enum ErrorCode {
    // Auth & Users
    INVALID_CREDENTIALS("Gecersiz kullanici adi veya sifre."),
    AUTHENTICATION_FAILED("Kimlik dogrulama basarisiz oldu."),
    ACCESS_DENIED("Bu islem icin yetkiniz yok."),
    USERNAME_ALREADY_EXISTS("Bu kullanici adi zaten kullanimda."),
    EMAIL_ALREADY_EXISTS("Bu e-posta adresi zaten kullanimda."),
    USER_NOT_FOUND("Kullanici bulunamadi."),
    USER_ROLE_NOT_FOUND("USER rolu bulunamadi."),
    USER_HAS_ORDERS("Bu kullanicinin siparis kayitlari oldugu icin silinemez."),
    INVALID_PASSWORD_POLICY("Sifre en az 8 karakter olmali, en az 1 buyuk harf ve 1 ozel karakter icermelidir."),
    UNAUTHENTICATED("Oturum acmis kullanici bilgisi bulunamadi."),
    USER_RESOLUTION_FAILED("Kullanici bilgisi cozumlenemedi."),

    // Cart
    NOT_ENOUGH_STOCK("Urun icin yeterli stok yok: %s"),
    PRODUCT_NOT_IN_CART("Urun sepette bulunamadi."),
    CART_EMPTY("Sepetiniz bos."),

    // Product & Category
    PRODUCT_NOT_FOUND("Urun bulunamadi."),
    PRODUCT_HAS_ORDER_ITEMS("Bu urun siparis gecmisinde oldugu icin silinemez."),
    CATEGORY_NOT_FOUND("Kategori bulunamadi."),
    CATEGORY_HAS_PRODUCTS("Bu kategoriye bagli urunler oldugu icin silinemez."),

    // Coupon
    COUPON_NOT_FOUND("Kupon bulunamadi."),
    COUPON_INACTIVE("Kupon aktif degil."),
    COUPON_NOT_STARTED("Kupon henuz aktif degil."),
    COUPON_EXPIRED("Kuponun suresi dolmus."),
    COUPON_USAGE_LIMIT_REACHED("Kupon kullanim limitine ulasmis."),
    COUPON_MIN_ORDER_NOT_MET("Kupon minimum siparis tutari kosulunu saglamiyor."),
    COUPON_NOT_ASSIGNED_TO_USER("Bu kupon size atanmis degil."),

    // Order & Checkout
    ORDER_NOT_FOUND("Siparis bulunamadi."),
    ORDER_NOT_PAYABLE("Bu siparis icin odeme yapilamaz."),
    INVALID_INSTALLMENT_COUNT("Taksit sayisi gecersiz."),
    PAYMENT_FAILED("Odeme basarisiz oldu."),

    // Global & Generic
    VALIDATION_ERROR("Istek verisi gecersiz."),
    DATA_INTEGRITY_VIOLATION("Iliskili kayitlar nedeniyle islem tamamlanamadi."),
    UNEXPECTED_ERROR("Beklenmeyen bir sunucu hatasi olustu."),
    INVALID_REQUEST("Gecersiz istek."),
    TOKEN_INVALID_OR_EXPIRED("Oturum suresi dolmus veya gecersiz token.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String formatMessage(Object... args) {
        return String.format(message, args);
    }
}