package com.cl.apps.mssepamd.controllers;

import com.cl.apps.mssepamd.exceptions.TransactionException;
import com.cl.apps.mssepamd.services.KX01Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de validation Jakarta pour ValidateIbanController.
 *
 * Stratégie : @WebMvcTest → charge uniquement la couche web
 * KX01Service est mocké → on teste UNIQUEMENT les validations du RequestBody
 *
 * Champs testés :
 *   - bic         : @NotBlank (B001) + @Size(max=11) (B002)
 *   - messageIdentification : @NotBlank (B001) + @Size(max=80) (B002)
 *   - iban        : @NotBlank (B001) + @Size(max=34) (B002)
 */
@WebMvcTest(ValidateIbanController.class)
@DisplayName("ValidateIbanController - Validation Jakarta")
class ValidateIbanControllerTest {

    private static final String URL = "/MSSEPAMD/diamondIban";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KX01Service kx01Service;

    // =========================================================================
    // Helpers
    // =========================================================================

    private String buildBody(String bic, String messageIdentification, String iban) {
        return """
            {
              "bic": %s,
              "messageIdentification": %s,
              "iban": %s
            }
            """.formatted(
                bic != null ? "\"" + bic + "\"" : "null",
                messageIdentification != null ? "\"" + messageIdentification + "\"" : "null",
                iban != null ? "\"" + iban + "\"" : "null"
        );
    }

    private String validBody() {
        return buildBody("CRLYFRPPXXX", "20221020175813703sTCvMgkN66", "FR4330002001010000005000R10");
    }

    // =========================================================================
    // Cas nominal
    // =========================================================================

    @Nested
    @DisplayName("Cas nominal")
    class NominalCase {

        @Test
        @DisplayName("Request valide → 200 OK")
        void should_return_200_when_request_is_valid() throws Exception {
            // GIVEN
            when(kx01Service.checkIbanValidated(any(), any(), any()))
                .thenReturn(null); // retour mocké

            // WHEN / THEN
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody()))
                .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // Validation : bic
    // =========================================================================

    @Nested
    @DisplayName("Validation champ 'bic'")
    class BicValidation {

        @Test
        @DisplayName("bic null → 400 + code B001")
        void should_return_B001_when_bic_is_null() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildBody(null, "20221020175813703sTCvMgkN66", "FR4330002001010000005000R10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B001"));
        }

        @Test
        @DisplayName("bic vide → 400 + code B001")
        void should_return_B001_when_bic_is_blank() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildBody("", "20221020175813703sTCvMgkN66", "FR4330002001010000005000R10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B001"));
        }

        @Test
        @DisplayName("bic > 11 caractères → 400 + code B002")
        void should_return_B002_when_bic_exceeds_max_size() throws Exception {
            // 12 caractères → dépasse max=11
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildBody("CRLYFRPPXXXX", "20221020175813703sTCvMgkN66", "FR4330002001010000005000R10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B002"));
        }

        @Test
        @DisplayName("bic = 11 caractères exactement → 200 OK (limite max acceptée)")
        void should_return_200_when_bic_is_exactly_max_size() throws Exception {
            when(kx01Service.checkIbanValidated(any(), any(), any())).thenReturn(null);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildBody("CRLYFRPPXXX", "20221020175813703sTCvMgkN66", "FR4330002001010000005000R10")))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("bic = 1 caractère → 200 OK (minimum valide)")
        void should_return_200_when_bic_is_one_char() throws Exception {
            when(kx01Service.checkIbanValidated(any(), any(), any())).thenReturn(null);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildBody("A", "20221020175813703sTCvMgkN66", "FR4330002001010000005000R10")))
                .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // Validation : messageIdentification
    // =========================================================================

    @Nested
    @DisplayName("Validation champ 'messageIdentification'")
    class MessageIdentificationValidation {

        @Test
        @DisplayName("messageIdentification null → 400 + code B001")
        void should_return_B001_when_messageIdentification_is_null() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildBody("CRLYFRPPXXX", null, "FR4330002001010000005000R10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B001"));
        }

        @Test
        @DisplayName("messageIdentification vide → 400 + code B001")
        void should_return_B001_when_messageIdentification_is_blank() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildBody("CRLYFRPPXXX", "   ", "FR4330002001010000005000R10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B001"));
        }

        @Test
        @DisplayName("messageIdentification > 80 caractères → 400 + code B002")
        void should_return_B002_when_messageIdentification_exceeds_max_size() throws Exception {
            String tooLong = "A".repeat(81); // 81 chars > max=80

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildBody("CRLYFRPPXXX", tooLong, "FR4330002001010000005000R10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B002"));
        }

        @Test
        @DisplayName("messageIdentification = 80 caractères → 200 OK (limite max acceptée)")
        void should_return_200_when_messageIdentification_is_exactly_max_size() throws Exception {
            when(kx01Service.checkIbanValidated(any(), any(), any())).thenReturn(null);
            String exactly80 = "A".repeat(80);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildBody("CRLYFRPPXXX", exactly80, "FR4330002001010000005000R10")))
                .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // Validation : iban
    // =========================================================================

    @Nested
    @DisplayName("Validation champ 'iban'")
    class IbanValidation {

        @Test
        @DisplayName("iban null → 400 + code B001")
        void should_return_B001_when_iban_is_null() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildBody("CRLYFRPPXXX", "20221020175813703sTCvMgkN66", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B001"));
        }

        @Test
        @DisplayName("iban vide → 400 + code B001")
        void should_return_B001_when_iban_is_blank() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildBody("CRLYFRPPXXX", "20221020175813703sTCvMgkN66", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B001"));
        }

        @Test
        @DisplayName("iban > 34 caractères → 400 + code B002")
        void should_return_B002_when_iban_exceeds_max_size() throws Exception {
            String tooLong = "FR" + "4".repeat(33); // 35 chars > max=34

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildBody("CRLYFRPPXXX", "20221020175813703sTCvMgkN66", tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B002"));
        }

        @Test
        @DisplayName("iban = 34 caractères → 200 OK (limite max acceptée)")
        void should_return_200_when_iban_is_exactly_max_size() throws Exception {
            when(kx01Service.checkIbanValidated(any(), any(), any())).thenReturn(null);
            String exactly34 = "FR" + "4".repeat(32); // 34 chars

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildBody("CRLYFRPPXXX", "20221020175813703sTCvMgkN66", exactly34)))
                .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // Body manquant / malformé
    // =========================================================================

    @Nested
    @DisplayName("Body manquant ou malformé")
    class MalformedRequest {

        @Test
        @DisplayName("Body vide → 400")
        void should_return_400_when_body_is_empty() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Content-Type absent → 415 Unsupported Media Type")
        void should_return_415_when_content_type_missing() throws Exception {
            mockMvc.perform(post(URL)
                    .content(validBody()))
                .andExpect(status().isUnsupportedMediaType());
        }
    }
}
