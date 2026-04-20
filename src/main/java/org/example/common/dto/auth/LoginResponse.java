package org.example.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.common.dto.permission.PermissionResponse;

import java.util.List;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String role;
    private List<PermissionResponse> permissions;
}
