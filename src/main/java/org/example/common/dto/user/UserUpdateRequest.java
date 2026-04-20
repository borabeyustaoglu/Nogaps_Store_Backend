package org.example.common.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String role;
}