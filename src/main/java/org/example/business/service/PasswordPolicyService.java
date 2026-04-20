package org.example.business.service;

import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class PasswordPolicyService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$");

    public void validateOrThrow(String rawPassword) {
        if (rawPassword == null || !PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_POLICY);
        }
    }
}
