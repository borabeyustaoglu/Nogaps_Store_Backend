package org.example.common.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class UserListItemResponse {
    private Integer id;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String role;
    private List<String> permissions;
}