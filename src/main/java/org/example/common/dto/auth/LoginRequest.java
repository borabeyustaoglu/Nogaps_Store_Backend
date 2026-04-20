package org.example.common.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Kullanici adi bos olamaz.")
    private String username;

    @NotBlank(message = "Sifre bos olamaz.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$",
            message = "Sifre en az 8 karakter olmali, en az 1 buyuk harf ve 1 ozel karakter icermelidir."
    )
    private String password;
}
