package com.cl.apps.mssepamd.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ExceptionsHandler.
 *
 * Organisation :
 *  - handleValidationErrors : cas nominal, cas null fieldError, cas multiples erreurs
 *  - extractCode            : avec séparateur, sans séparateur, null/vide
 *  - extractMessage         : avec séparateur, sans séparateur, null/vide
 */
@DisplayName("ExceptionsHandler")
class ExceptionsHandlerTest {

    private ExceptionsHandler handler;

    // CODE_SEPARATOR tel que défini dans la classe (ex: "|")
    // À adapter si la constante est différente
    private static final String SEP = "|";

    @BeforeEach
    void setUp() {
        handler = new ExceptionsHandler();
        MDC.clear();
    }

    // =========================================================================
    // handleValidationErrors
    // =========================================================================

    @Nested
    @DisplayName("handleValidationErrors()")
    class HandleValidationErrors {

        @Test
        @DisplayName("Cas nominal : un FieldError avec code et message valides")
        void shouldReturnBadRequestWithExtractedCodeAndMessage() {
            // GIVEN
            String rawMessage = "B002" + SEP + "IBAN invalide";
            FieldError fieldError = new FieldError("request", "iban", rawMessage);

            MethodArgumentNotValidException ex = buildException(List.of(fieldError));

            // WHEN
            ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);

            // THEN
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("B002");
            assertThat(response.getBody().message()).isEqualTo("IBAN invalide");
        }

        @Test
        @DisplayName("Cas nominal : FieldError sans séparateur → fallback code B001, message brut")
        void shouldReturnDefaultCodeWhenNoSeparatorInMessage() {
            // GIVEN
            String rawMessage = "champ obligatoire";
            FieldError fieldError = new FieldError("request", "bic", rawMessage);

            MethodArgumentNotValidException ex = buildException(List.of(fieldError));

            // WHEN
            ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);

            // THEN
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("B001");
            assertThat(response.getBody().message()).isEqualTo("champ obligatoire");
        }

        @Test
        @DisplayName("Cas null FieldError : aucun field error dans le BindingResult")
        void shouldReturnDefaultErrorWhenNoFieldErrors() {
            // GIVEN
            MethodArgumentNotValidException ex = buildException(Collections.emptyList());

            // WHEN
            ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);

            // THEN
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("B001");
            assertThat(response.getBody().message()).isEqualTo("Validation error");
        }

        @Test
        @DisplayName("Plusieurs FieldErrors : le .min() doit sélectionner le code le plus petit alphabétiquement")
        void shouldSelectFieldErrorWithMinCode_whenMultipleErrors() {
            // GIVEN
            FieldError errorB003 = new FieldError("request", "iban", "B003" + SEP + "IBAN trop court");
            FieldError errorB001 = new FieldError("request", "bic",  "B001" + SEP + "BIC manquant");
            FieldError errorB002 = new FieldError("request", "ref",  "B002" + SEP + "Référence invalide");

            MethodArgumentNotValidException ex = buildException(List.of(errorB003, errorB001, errorB002));

            // WHEN
            ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);

            // THEN — B001 < B002 < B003 alphabétiquement
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("B001");
            assertThat(response.getBody().message()).isEqualTo("BIC manquant");
        }

        @Test
        @DisplayName("MDC : layer_id et operation_status sont positionnés pendant le traitement")
        void shouldPopulateMdcDuringProcessing() {
            // GIVEN
            String rawMessage = "B002" + SEP + "IBAN invalide";
            FieldError fieldError = new FieldError("request", "iban", rawMessage);
            MethodArgumentNotValidException ex = buildException(List.of(fieldError));

            // WHEN
            handler.handleValidationErrors(ex);

            // THEN — le MDC est positionné (non nettoyé ici car pas de finally dans l'implémentation actuelle)
            // Ce test documente le comportement actuel ; à adapter si un finally est ajouté
            assertThat(MDC.get("layer_id")).isEqualTo("Controller");
            assertThat(MDC.get("operation_status")).isEqualTo("ko");
        }

        @Test
        @DisplayName("FieldError avec message null → fallback B001 / Validation error")
        void shouldHandleNullDefaultMessage() {
            // GIVEN
            FieldError fieldError = new FieldError("request", "iban", null,
                    false, null, null, null);
            MethodArgumentNotValidException ex = buildException(List.of(fieldError));

            // WHEN
            ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);

            // THEN
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("B001");
        }
    }

    // =========================================================================
    // extractCode (testé via handleValidationErrors pour couvrir les branches)
    // =========================================================================

    @Nested
    @DisplayName("extractCode() — via handleValidationErrors")
    class ExtractCode {

        @Test
        @DisplayName("Message avec séparateur → retourne la partie gauche")
        void shouldExtractCodeBeforeSeparator() {
            FieldError fe = new FieldError("o", "f", "B099" + SEP + "msg");
            ResponseEntity<ErrorResponse> response =
                    handler.handleValidationErrors(buildException(List.of(fe)));

            assertThat(response.getBody().code()).isEqualTo("B099");
        }

        @ParameterizedTest(name = "rawMessage=''{0}''")
        @NullAndEmptySource
        @DisplayName("Message null ou vide → fallback B001")
        void shouldReturnDefaultCodeForNullOrEmpty(String rawMessage) {
            FieldError fe = new FieldError("o", "f", rawMessage,
                    false, null, null, rawMessage);
            ResponseEntity<ErrorResponse> response =
                    handler.handleValidationErrors(buildException(List.of(fe)));

            assertThat(response.getBody().code()).isEqualTo("B001");
        }

        @Test
        @DisplayName("Message sans séparateur → fallback B001")
        void shouldReturnDefaultCodeWhenNoSeparator() {
            FieldError fe = new FieldError("o", "f", "message sans separateur");
            ResponseEntity<ErrorResponse> response =
                    handler.handleValidationErrors(buildException(List.of(fe)));

            assertThat(response.getBody().code()).isEqualTo("B001");
        }
    }

    // =========================================================================
    // extractMessage (testé via handleValidationErrors pour couvrir les branches)
    // =========================================================================

    @Nested
    @DisplayName("extractMessage() — via handleValidationErrors")
    class ExtractMessage {

        @Test
        @DisplayName("Message avec séparateur → retourne la partie droite")
        void shouldExtractMessageAfterSeparator() {
            FieldError fe = new FieldError("o", "f", "B002" + SEP + "IBAN invalide");
            ResponseEntity<ErrorResponse> response =
                    handler.handleValidationErrors(buildException(List.of(fe)));

            assertThat(response.getBody().message()).isEqualTo("IBAN invalide");
        }

        @Test
        @DisplayName("Message sans séparateur → retourne le message brut complet")
        void shouldReturnRawMessageWhenNoSeparator() {
            String rawMessage = "message brut sans code";
            FieldError fe = new FieldError("o", "f", rawMessage);
            ResponseEntity<ErrorResponse> response =
                    handler.handleValidationErrors(buildException(List.of(fe)));

            assertThat(response.getBody().message()).isEqualTo(rawMessage);
        }

        @Test
        @DisplayName("Message avec séparateur multiple → retourne tout après le premier séparateur")
        void shouldReturnEverythingAfterFirstSeparator() {
            // ex: "B002|message avec | dedans"
            FieldError fe = new FieldError("o", "f", "B002" + SEP + "msg avec " + SEP + " dedans");
            ResponseEntity<ErrorResponse> response =
                    handler.handleValidationErrors(buildException(List.of(fe)));

            assertThat(response.getBody().message()).isEqualTo("msg avec " + SEP + " dedans");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Construit un MethodArgumentNotValidException mocké avec la liste de FieldErrors fournie.
     */
    private MethodArgumentNotValidException buildException(List<FieldError> fieldErrors) {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        return ex;
    }
}
