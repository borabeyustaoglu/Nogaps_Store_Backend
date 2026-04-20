package org.example.api.controller;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<MessageResponse> handleAppException(AppException ex) {
        HttpStatus status = resolveStatus(ex.getErrorCode());
        String message = ex.getMessage() == null || ex.getMessage().isBlank()
                ? defaultMessageForStatus(status)
                : ex.getMessage();
        return ResponseEntity.status(status).body(new MessageResponse(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String detail = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Gonderilen bilgiler gecersiz."
                : ex.getMessage();
        return ResponseEntity.badRequest().body(new MessageResponse(detail));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> {
                    String detail = error.getDefaultMessage();
                    if (detail == null || detail.isBlank()) {
                        return "Gonderilen bilgiler gecersiz.";
                    }
                    return error.getField() + " alani gecersiz: " + detail;
                })
                .orElse("Gonderilen bilgiler gecersiz.");
        return ResponseEntity.badRequest().body(new MessageResponse(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<MessageResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(new MessageResponse("Gonderilen bilgiler dogrulanamadi."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<MessageResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(new MessageResponse("Zorunlu bir alan eksik: " + ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<MessageResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String field = ex.getName() == null ? "Bir alan" : ex.getName();
        return ResponseEntity.badRequest().body(new MessageResponse(field + " alani hatali tipte gonderildi."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MessageResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new MessageResponse("Istek govdesi okunamadi. Alanlari kontrol edin."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<MessageResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new MessageResponse("Bu istek metodu desteklenmiyor."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<MessageResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new MessageResponse("Gonderilen icerik tipi desteklenmiyor."));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Kullanici adi veya sifre hatali."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<MessageResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Kimlik dogrulama basarisiz oldu. Lutfen tekrar giris yapin."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MessageResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse("Bu islem icin yetkiniz yok."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MessageResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String technical = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
        String normalized = technical == null ? "" : technical.toLowerCase();

        String message;
        if (normalized.contains("duplicate") || normalized.contains("unique")) {
            message = "Bu kayit zaten mevcut.";
        } else if (normalized.contains("foreign key") || normalized.contains("constraint")) {
            message = "Kayit baska verilerle iliskili oldugu icin islem tamamlanamadi.";
        } else {
            message = "Veri butunlugu nedeniyle islem tamamlanamadi.";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponse(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("Sunucuda beklenmeyen bir hata olustu. Lutfen tekrar deneyin."));
    }

    private HttpStatus resolveStatus(ErrorCode errorCode) {
        if (errorCode == null) {
            return HttpStatus.BAD_REQUEST;
        }

        return switch (errorCode) {
            case AUTHENTICATION_FAILED, INVALID_CREDENTIALS, UNAUTHENTICATED, TOKEN_INVALID_OR_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case USER_NOT_FOUND, PRODUCT_NOT_FOUND, CATEGORY_NOT_FOUND, ORDER_NOT_FOUND, COUPON_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DATA_INTEGRITY_VIOLATION, PRODUCT_HAS_ORDER_ITEMS -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private String defaultMessageForStatus(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> "Oturumunuz dogrulanamadi. Lutfen tekrar giris yapin.";
            case FORBIDDEN -> "Bu islem icin yetkiniz yok.";
            case NOT_FOUND -> "Istenen kayit bulunamadi.";
            case CONFLICT -> "Islem mevcut kayitlarla cakistigi icin tamamlanamadi.";
            default -> "Gonderilen bilgiler gecersiz veya eksik.";
        };
    }
}