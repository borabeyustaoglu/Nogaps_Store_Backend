package org.example.business.service;

import com.iyzipay.Options;
import com.iyzipay.model.Address;
import com.iyzipay.model.BasketItem;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.Buyer;
import com.iyzipay.model.Currency;
import com.iyzipay.model.Locale;
import com.iyzipay.model.Payment;
import com.iyzipay.model.PaymentCard;
import com.iyzipay.model.PaymentChannel;
import com.iyzipay.model.PaymentGroup;
import com.iyzipay.request.CreatePaymentRequest;
import org.example.common.dto.checkout.PaymentRequest;
import org.example.common.entity.Order;
import org.example.common.entity.OrderItem;
import org.example.common.entity.ShippingAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class IyzicoPaymentService {

    private static final DateTimeFormatter IYZICO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${app.payment.iyzico.enabled:false}")
    private boolean enabled;

    @Value("${app.payment.iyzico.api-key:}")
    private String apiKey;

    @Value("${app.payment.iyzico.secret-key:}")
    private String secretKey;

    @Value("${app.payment.iyzico.base-url:https://sandbox-api.iyzipay.com}")
    private String baseUrl;

    @Value("${app.payment.iyzico.buyer-ip:127.0.0.1}")
    private String buyerIp;

    @Value("${app.payment.iyzico.identity-number:11111111111}")
    private String defaultIdentityNumber;

    @Value("${app.payment.iyzico.test-card-holder:John Doe}")
    private String defaultCardHolder;

    @Value("${app.payment.iyzico.test-card-number:5528790000000008}")
    private String defaultCardNumber;

    @Value("${app.payment.iyzico.test-expire-month:12}")
    private String defaultExpireMonth;

    @Value("${app.payment.iyzico.test-expire-year:2030}")
    private String defaultExpireYear;

    @Value("${app.payment.iyzico.test-cvc:123}")
    private String defaultCvc;

    public IyzicoPaymentResult charge(Order order, PaymentRequest paymentRequest) {
        if (!enabled) {
            return IyzicoPaymentResult.failed("IYZICO_DISABLED", "Iyzico sandbox aktif degil.");
        }
        if (isBlank(apiKey) || isBlank(secretKey)) {
            return IyzicoPaymentResult.failed("IYZICO_CONFIG_MISSING", "Iyzico API anahtarlari eksik.");
        }

        try {
            Options options = new Options();
            options.setApiKey(apiKey);
            options.setSecretKey(secretKey);
            options.setBaseUrl(baseUrl);

            CreatePaymentRequest request = buildRequest(order, paymentRequest);
            Payment payment = Payment.create(request, options);
            String status = payment.getStatus();

            if ("success".equalsIgnoreCase(status)) {
                String txId = firstNonBlank(payment.getPaymentId(), payment.getConversationId(), order.getOrderNumber());
                return IyzicoPaymentResult.success(txId);
            }

            String errorCode = firstNonBlank(payment.getErrorCode(), "IYZICO_PAYMENT_FAILED");
            String errorMessage = firstNonBlank(payment.getErrorMessage(), "Iyzico odeme islemi basarisiz.");
            return IyzicoPaymentResult.failed(errorCode, errorMessage);
        } catch (Exception ex) {
            return IyzicoPaymentResult.failed("IYZICO_EXCEPTION", ex.getMessage());
        }
    }

    private CreatePaymentRequest buildRequest(Order order, PaymentRequest paymentRequest) {
        ShippingAddress shippingAddress = order.getShippingAddress();

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setLocale(Locale.TR.getValue());
        request.setConversationId(order.getOrderNumber());
        request.setPrice(toMoney(order.getGrandTotal()));
        request.setPaidPrice(toMoney(order.getGrandTotal()));
        request.setInstallment(order.getInstallmentCount() == null ? 1 : order.getInstallmentCount());
        request.setBasketId(order.getOrderNumber());
        request.setPaymentChannel(PaymentChannel.WEB.name());
        request.setPaymentGroup(PaymentGroup.PRODUCT.name());
        request.setCurrency(Currency.TRY.name());

        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setCardHolderName(firstNonBlank(nullSafe(paymentRequest, PaymentRequest::getCardHolderName), defaultCardHolder));
        paymentCard.setCardNumber(firstNonBlank(nullSafe(paymentRequest, PaymentRequest::getCardNumber), defaultCardNumber));
        paymentCard.setExpireMonth(firstNonBlank(nullSafe(paymentRequest, PaymentRequest::getExpireMonth), defaultExpireMonth));
        paymentCard.setExpireYear(firstNonBlank(nullSafe(paymentRequest, PaymentRequest::getExpireYear), defaultExpireYear));
        paymentCard.setCvc(firstNonBlank(nullSafe(paymentRequest, PaymentRequest::getCvc), defaultCvc));
        paymentCard.setRegisterCard(0);
        request.setPaymentCard(paymentCard);

        Buyer buyer = new Buyer();
        buyer.setId("USR-" + order.getUser().getId());
        buyer.setName(extractFirstName(order.getUser().getFullName()));
        buyer.setSurname(extractLastName(order.getUser().getFullName()));
        buyer.setGsmNumber(normalizePhone(order.getUser().getPhoneNumber()));
        buyer.setEmail(order.getUser().getEmail());
        buyer.setIdentityNumber(defaultIdentityNumber);
        String now = LocalDateTime.now().format(IYZICO_DATE);
        buyer.setLastLoginDate(now);
        buyer.setRegistrationDate(now);
        buyer.setRegistrationAddress(firstNonBlank(order.getUser().getAddress(), "Address not provided"));
        buyer.setIp(firstNonBlank(buyerIp, "127.0.0.1"));
        buyer.setCity(firstNonBlank(shippingAddress != null ? shippingAddress.getCity() : null, "Istanbul"));
        buyer.setCountry(firstNonBlank(shippingAddress != null ? shippingAddress.getCountry() : null, "Turkey"));
        buyer.setZipCode(firstNonBlank(shippingAddress != null ? shippingAddress.getPostalCode() : null, "34000"));
        request.setBuyer(buyer);

        Address shipping = new Address();
        shipping.setContactName(firstNonBlank(shippingAddress != null ? shippingAddress.getFullName() : null, order.getUser().getFullName()));
        shipping.setCity(firstNonBlank(shippingAddress != null ? shippingAddress.getCity() : null, "Istanbul"));
        shipping.setCountry(firstNonBlank(shippingAddress != null ? shippingAddress.getCountry() : null, "Turkey"));
        shipping.setAddress(firstNonBlank(shippingAddress != null ? shippingAddress.getAddressLine() : null, order.getUser().getAddress()));
        shipping.setZipCode(firstNonBlank(shippingAddress != null ? shippingAddress.getPostalCode() : null, "34000"));
        request.setShippingAddress(shipping);

        Address billing = new Address();
        billing.setContactName(firstNonBlank(order.getUser().getFullName(), shippingAddress != null ? shippingAddress.getFullName() : null));
        billing.setCity(firstNonBlank(shippingAddress != null ? shippingAddress.getCity() : null, "Istanbul"));
        billing.setCountry(firstNonBlank(shippingAddress != null ? shippingAddress.getCountry() : null, "Turkey"));
        billing.setAddress(firstNonBlank(order.getUser().getAddress(), shippingAddress != null ? shippingAddress.getAddressLine() : null));
        billing.setZipCode(firstNonBlank(shippingAddress != null ? shippingAddress.getPostalCode() : null, "34000"));
        request.setBillingAddress(billing);

        List<BasketItem> basketItems = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            BasketItem basketItem = new BasketItem();
            basketItem.setId("ITEM-" + item.getId());
            basketItem.setName(firstNonBlank(item.getProductName(), "Product"));
            basketItem.setCategory1(item.getProduct() != null && item.getProduct().getCategory() != null
                    ? item.getProduct().getCategory().getName()
                    : "General");
            basketItem.setItemType(BasketItemType.PHYSICAL.name());
            basketItem.setPrice(toMoney(item.getLineTotal()));
            basketItems.add(basketItem);
        }
        request.setBasketItems(basketItems);

        return request;
    }

    private String toMoney(java.math.BigDecimal amount) {
        java.math.BigDecimal safe = amount == null ? java.math.BigDecimal.ZERO : amount;
        return safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String extractFirstName(String fullName) {
        String source = firstNonBlank(fullName, "Demo User").trim();
        int idx = source.indexOf(' ');
        return idx < 0 ? source : source.substring(0, idx);
    }

    private String extractLastName(String fullName) {
        String source = firstNonBlank(fullName, "Demo User").trim();
        int idx = source.indexOf(' ');
        return idx < 0 ? "User" : source.substring(idx + 1);
    }

    private String normalizePhone(String phone) {
        String source = firstNonBlank(phone, "+905555555555");
        String digits = source.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) {
            return digits;
        }
        if (digits.startsWith("90")) {
            return "+" + digits;
        }
        return "+90" + digits;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private <T> String nullSafe(T source, java.util.function.Function<T, String> getter) {
        if (source == null) {
            return null;
        }
        return getter.apply(source);
    }

    public record IyzicoPaymentResult(boolean success, String transactionId, String errorCode, String errorMessage) {
        public static IyzicoPaymentResult success(String transactionId) {
            return new IyzicoPaymentResult(true, transactionId, null, null);
        }

        public static IyzicoPaymentResult failed(String errorCode, String errorMessage) {
            return new IyzicoPaymentResult(false, null, errorCode, errorMessage);
        }
    }
}
