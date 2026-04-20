package org.example.common.dto.checkout;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShippingAddressRequest {

    @NotBlank(message = "Teslimat tam adi zorunludur.")
    private String fullName;

    @NotBlank(message = "Telefon numarasi zorunludur.")
    private String phoneNumber;

    @NotBlank(message = "Sehir zorunludur.")
    private String city;

    @NotBlank(message = "Ilce zorunludur.")
    private String district;

    @NotBlank(message = "Adres satiri zorunludur.")
    private String addressLine;

    private String postalCode;

    private String country;
}