package com.lcl.wsctxd04.exceptions;

import com.lcl.wsctxd04.models.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gestion centralisée des exceptions.
 *
 * Codes de retour validation :
 * - B001 (400) : champ obligatoire absent ou vide
 * - B002 (400) : champ présent mais dépasse la taille maximale
 * - UNKNOWN_ERROR (500) : erreur technique CICS
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CODE_SEPARATOR = ":";

    /**
     * Validation @Valid → retourne 400 avec le code B001 ou B002.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {

        var fieldError = ex.getBindingResult().getFieldErrors().stream()
                .min((a, b) -> extractCode(a.getDefaultMessage())
                        .compareTo(extractCode(b.getDefaultMessage())))
                .orElse(null);

        if (fieldError == null) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("B001", "Validation error"));
        }

        String code = extractCode(fieldError.getDefaultMessage());
        String message = extractMessage(fieldError.getDefaultMessage());

        log.warn("Validation failed [{}] on field '{}': {}", code, fieldError.getField(), message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(code, message));
    }

    /**
     * Erreur interne WSCTXD04 → 500.
     */
    @ExceptionHandler(InternalWSCTXD04Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalException(InternalWSCTXD04Exception ex) {
        log.error("CICS communication error: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("UNKNOWN_ERROR",
                        "Error occurred while communicating with CICS Service. "
                                + "Please ensure that the CICS Service is operational."));
    }

    /**
     * Erreur transactionnelle CICS → 500 avec le code spécifique.
     */
    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<ErrorResponse> handleTransactionException(TransactionException ex) {
        log.error("Transaction error [{}]: {}", ex.getCode(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    /**
     * Erreur service métier → 400 avec le code spécifique.
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException ex) {
        log.error("Service error [{}]: {}", ex.getCode(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    /**
     * Filet de sécurité → 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("UNKNOWN_ERROR", "An unexpected error occurred."));
    }

    private String extractCode(String rawMessage) {
        if (rawMessage != null && rawMessage.contains(CODE_SEPARATOR)) {
            return rawMessage.substring(0, rawMessage.indexOf(CODE_SEPARATOR));
        }
        return "B001";
    }

    private String extractMessage(String rawMessage) {
        if (rawMessage != null && rawMessage.contains(CODE_SEPARATOR)) {
            return rawMessage.substring(rawMessage.indexOf(CODE_SEPARATOR) + 1);
        }
        return rawMessage;
    }
}
