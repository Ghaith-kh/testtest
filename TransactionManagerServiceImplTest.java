package com.cl.apps.mssepamd.services;

import com.cl.apps.mssepamd.exceptions.RetourCICS;
import com.cl.apps.mssepamd.exceptions.TransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour TransactionManagerServiceImpl.verifierErreur()
 *
 * Logique couverte :
 *  1. codeSysteme non blank ET != ERROR_CODE → TransactionException (code = libelleSysteme)
 *  2. codeApplicatif non blank ET != ERROR_CODE → TransactionException (code = libelleApplicatif)
 *  3. codeSortieAppli OU libelleSortieAppli non blank → TransactionException
 *  4. Aucune condition remplie → pas d'exception
 */
@DisplayName("TransactionManagerServiceImpl - verifierErreur()")
class TransactionManagerServiceImplTest {

    // Instance réelle — pas de mock, on teste la vraie logique métier
    private TransactionManagerServiceImpl service;

    // La constante ERROR_CODE_SYSTEM_OR_APPLICATION telle que définie dans la classe
    // À adapter si la valeur est différente
    private static final String ERROR_CODE = "0000";

    @BeforeEach
    void setUp() {
        service = new TransactionManagerServiceImpl();
    }

    // =========================================================================
    // Branche 1 : codeSysteme
    // =========================================================================

    @Nested
    @DisplayName("Branche codeSysteme")
    class CodeSysteme {

        @Test
        @DisplayName("codeSysteme != 0000 → TransactionException avec libelleSysteme comme code")
        void should_throw_with_libelleSysteme_when_codeSysteme_is_not_0000() {
            // GIVEN
            RetourCICS retourCICS = RetourCICS.builder()
                .codeSysteme("0001")
                .libelleSysteme("Nullpointer exception")
                .messageDetaille("Erreur lors du traitement système")
                .build();

            // WHEN
            TransactionException ex = assertThrows(TransactionException.class,
                () -> service.verifierErreur(retourCICS));

            // THEN
            assertAll(
                () -> assertThat(ex).isNotNull(),
                () -> assertThat(ex.getCode()).isEqualTo("Nullpointer exception"),
                () -> assertThat(ex.getMessage()).isEqualTo("Erreur lors du traitement système")
            );
        }

        @Test
        @DisplayName("codeSysteme = 0000 → pas d'exception sur cette branche")
        void should_not_throw_on_codeSysteme_branch_when_code_is_0000() {
            // GIVEN — codeSysteme = 0000, les autres champs vides
            RetourCICS retourCICS = RetourCICS.builder()
                .codeSysteme(ERROR_CODE)
                .build();

            // THEN — aucune exception levée
            assertDoesNotThrow(() -> service.verifierErreur(retourCICS));
        }

        @Test
        @DisplayName("codeSysteme blank → branche ignorée, pas d'exception")
        void should_not_throw_when_codeSysteme_is_blank() {
            RetourCICS retourCICS = RetourCICS.builder()
                .codeSysteme("   ")
                .build();

            assertDoesNotThrow(() -> service.verifierErreur(retourCICS));
        }

        @Test
        @DisplayName("codeSysteme null → branche ignorée, pas d'exception")
        void should_not_throw_when_codeSysteme_is_null() {
            RetourCICS retourCICS = RetourCICS.builder()
                .codeSysteme(null)
                .build();

            assertDoesNotThrow(() -> service.verifierErreur(retourCICS));
        }
    }

    // =========================================================================
    // Branche 2 : codeApplicatif
    // =========================================================================

    @Nested
    @DisplayName("Branche codeApplicatif")
    class CodeApplicatif {

        @Test
        @DisplayName("codeApplicatif != 0000 → TransactionException avec libelleApplicatif comme code")
        void should_throw_with_libelleApplicatif_when_codeApplicatif_is_not_0000() {
            // GIVEN
            RetourCICS retourCICS = RetourCICS.builder()
                .codeSysteme(ERROR_CODE)           // branche 1 ignorée
                .codeApplicatif("0001")
                .libelleApplicatif("Erreur applicative")
                .messageDetaille("Erreur lors du traitement applicatif")
                .build();

            // WHEN
            TransactionException ex = assertThrows(TransactionException.class,
                () -> service.verifierErreur(retourCICS));

            // THEN
            assertAll(
                () -> assertThat(ex.getCode()).isEqualTo("Erreur applicative"),
                () -> assertThat(ex.getMessage()).isEqualTo("Erreur lors du traitement applicatif")
            );
        }

        @Test
        @DisplayName("codeApplicatif = 0000 → pas d'exception sur cette branche")
        void should_not_throw_when_codeApplicatif_is_0000() {
            RetourCICS retourCICS = RetourCICS.builder()
                .codeApplicatif(ERROR_CODE)
                .build();

            assertDoesNotThrow(() -> service.verifierErreur(retourCICS));
        }

        @Test
        @DisplayName("codeApplicatif blank → branche ignorée")
        void should_not_throw_when_codeApplicatif_is_blank() {
            RetourCICS retourCICS = RetourCICS.builder()
                .codeApplicatif("")
                .build();

            assertDoesNotThrow(() -> service.verifierErreur(retourCICS));
        }
    }

    // =========================================================================
    // Branche 3 : codeSortieAppli / libelleSortieAppli
    // =========================================================================

    @Nested
    @DisplayName("Branche codeSortieAppli / libelleSortieAppli")
    class CodeSortieAppli {

        @Test
        @DisplayName("codeSortieAppli non blank → TransactionException")
        void should_throw_when_codeSortieAppli_is_filled() {
            // GIVEN
            RetourCICS retourCICS = RetourCICS.builder()
                .codeSortieAppli("ERR_PROCESSING")
                .libelleSortieAppli("Erreur de sortie applicative")
                .build();

            // WHEN
            TransactionException ex = assertThrows(TransactionException.class,
                () -> service.verifierErreur(retourCICS));

            // THEN
            assertAll(
                () -> assertThat(ex.getCode()).isEqualTo("ERR_PROCESSING"),
                () -> assertThat(ex.getMessage()).isEqualTo("Erreur de sortie applicative")
            );
        }

        @Test
        @DisplayName("libelleSortieAppli non blank (sans codeSortieAppli) → TransactionException")
        void should_throw_when_libelleSortieAppli_is_filled_alone() {
            // GIVEN — condition OR : libelleSortieAppli seul suffit
            RetourCICS retourCICS = RetourCICS.builder()
                .codeSortieAppli("")
                .libelleSortieAppli("Message sortie applicative")
                .build();

            assertThrows(TransactionException.class,
                () -> service.verifierErreur(retourCICS));
        }

        @Test
        @DisplayName("codeSortieAppli et libelleSortieAppli vides → pas d'exception")
        void should_not_throw_when_both_sortie_fields_are_blank() {
            RetourCICS retourCICS = RetourCICS.builder()
                .codeSortieAppli("")
                .libelleSortieAppli("")
                .build();

            assertDoesNotThrow(() -> service.verifierErreur(retourCICS));
        }
    }

    // =========================================================================
    // Cas nominal : aucune erreur
    // =========================================================================

    @Nested
    @DisplayName("Cas nominal — aucune erreur CICS")
    class NominalCase {

        @Test
        @DisplayName("Tous les champs à 0000 ou vides → aucune exception")
        void should_not_throw_when_all_codes_are_ok() {
            RetourCICS retourCICS = RetourCICS.builder()
                .codeSysteme(ERROR_CODE)
                .codeApplicatif(ERROR_CODE)
                .codeSortieAppli("")
                .libelleSortieAppli("")
                .build();

            assertDoesNotThrow(() -> service.verifierErreur(retourCICS));
        }

        @Test
        @DisplayName("TransactionException a toujours le status INTERNAL_SERVER_ERROR")
        void should_have_internal_server_error_status_on_exception() {
            RetourCICS retourCICS = RetourCICS.builder()
                .codeSysteme("0001")
                .libelleSysteme("Erreur")
                .build();

            TransactionException ex = assertThrows(TransactionException.class,
                () -> service.verifierErreur(retourCICS));

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
