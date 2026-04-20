package org.example.common.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Kullanici adi bos olamaz.")
    private String username;

    @NotBlank(message = "Sifre bos olamaz.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$",
            message = "Sifre en az 8 karakter olmali, en az 1 buyuk harf ve 1 ozel karakter icermelidir."
    )
    private String password;

    @NotBlank(message = "Ad soyad bos olamaz.")
    private String fullName;

    @NotBlank(message = "E-posta bos olamaz.")
    @Email(message = "Gecerli bir e-posta adresi giriniz.")
    private String email;

    @NotBlank(message = "Telefon numarasi bos olamaz.")
    private String phoneNumber;

    @NotBlank(message = "Adres bos olamaz.")
    private String address;
}
