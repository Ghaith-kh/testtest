package com.cl.apps.mssepamd.services.impl;

import com.cl.apps.mssepamd.exceptions.TransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link TransactionManagerServiceImpl}.
 *
 * <p>Stratégie de test :
 * <ul>
 *   <li>verifierErreur : couverture exhaustive des 4 branches (codeSysteme, codeApplicatif,
 *       codeSortieAppli, aucune erreur)</li>
 *   <li>callTransaction : vérification de l'orchestration des appels CICS</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionManagerServiceImpl")
class TransactionManagerServiceImplTest {

    @Mock
    private CICSService cics;

    @Mock
    private CICSProperties cicsProperties;

    @InjectMocks
    private TransactionManagerServiceImpl service;

    // -------------------------------------------------------------------------
    // Helpers – construction d'un RetourCICS stub
    // -------------------------------------------------------------------------

    /**
     * Crée un {@link RetourCICS} totalement vide (aucune erreur).
     */
    private RetourCICS retourSansErreur() {
        RetourCICS retour = mock(RetourCICS.class);
        lenient().when(retour.getCodeSysteme()).thenReturn(null);
        lenient().when(retour.getCodeApplicatif()).thenReturn(null);
        lenient().when(retour.getCodeSortieAppli()).thenReturn(null);
        lenient().when(retour.getLibelleSortieAppli()).thenReturn(null);
        return retour;
    }

    // =========================================================================
    // verifierErreur
    // =========================================================================

    @Nested
    @DisplayName("verifierErreur()")
    class VerifierErreurTests {

        // -----------------------------------------------------------------
        // Branche 1 : codeSysteme présent et != "0000"
        // -----------------------------------------------------------------

        @Test
        @DisplayName("doit lever TransactionException quand codeSysteme est non-blank et != '0000'")
        void verifierErreur_codeSystemeNonBlankEtDifferentDe0000_leveException() {
            // GIVEN
            RetourCICS erreur = mock(RetourCICS.class);
            when(erreur.getCodeSysteme()).thenReturn("SYS1");
            when(erreur.getLibelleSysteme()).thenReturn("Erreur système grave");
            when(erreur.getMessageDetaille()).thenReturn("Détail erreur système");

            // WHEN / THEN
            assertThatThrownBy(() -> service.verifierErreur(erreur))
                    .isInstanceOf(TransactionException.class)
                    .satisfies(ex -> {
                        TransactionException txEx = (TransactionException) ex;
                        assertThat(txEx.getCode()).isEqualTo("Erreur système grave");
                        assertThat(txEx.getMessage()).isEqualTo("Détail erreur système");
                        assertThat(txEx.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }

        @Test
        @DisplayName("ne doit PAS lever d'exception quand codeSysteme vaut '0000' (valeur nominale)")
        void verifierErreur_codeSystemeEgalA0000_pasException() {
            // GIVEN – "0000" == ERROR_CODE_SYSTEM_OR_APPLICATION → branche ignorée
            RetourCICS erreur = mock(RetourCICS.class);
            when(erreur.getCodeSysteme()).thenReturn("0000");
            when(erreur.getCodeApplicatif()).thenReturn(null);
            when(erreur.getCodeSortieAppli()).thenReturn(null);
            when(erreur.getLibelleSortieAppli()).thenReturn(null);

            // WHEN / THEN
            assertThatNoException().isThrownBy(() -> service.verifierErreur(erreur));
        }

        @Test
        @DisplayName("ne doit PAS lever d'exception quand codeSysteme est blank")
        void verifierErreur_codeSystemeBlank_pasException() {
            // GIVEN
            RetourCICS erreur = mock(RetourCICS.class);
            when(erreur.getCodeSysteme()).thenReturn("   ");
            when(erreur.getCodeApplicatif()).thenReturn(null);
            when(erreur.getCodeSortieAppli()).thenReturn(null);
            when(erreur.getLibelleSortieAppli()).thenReturn(null);

            // WHEN / THEN
            assertThatNoException().isThrownBy(() -> service.verifierErreur(erreur));
        }

        // -----------------------------------------------------------------
        // Branche 2 : codeApplicatif présent et != "0000"
        // -----------------------------------------------------------------

        @Test
        @DisplayName("doit lever TransactionException quand codeApplicatif est non-blank et != '0000'")
        void verifierErreur_codeApplicatifNonBlankEtDifferentDe0000_leveException() {
            // GIVEN – codeSysteme ignoré (null), codeApplicatif déclenche l'erreur
            RetourCICS erreur = mock(RetourCICS.class);
            when(erreur.getCodeSysteme()).thenReturn(null);
            when(erreur.getCodeApplicatif()).thenReturn("APP9");
            when(erreur.getLibelleApplicatif()).thenReturn("Erreur applicative");
            when(erreur.getMessageDetaille()).thenReturn("Détail erreur applicative");

            // WHEN / THEN
            assertThatThrownBy(() -> service.verifierErreur(erreur))
                    .isInstanceOf(TransactionException.class)
                    .satisfies(ex -> {
                        TransactionException txEx = (TransactionException) ex;
                        assertThat(txEx.getCode()).isEqualTo("Erreur applicative");
                        assertThat(txEx.getMessage()).isEqualTo("Détail erreur applicative");
                        assertThat(txEx.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }

        @Test
        @DisplayName("ne doit PAS lever d'exception quand codeApplicatif vaut '0000'")
        void verifierErreur_codeApplicatifEgalA0000_pasException() {
            // GIVEN
            RetourCICS erreur = mock(RetourCICS.class);
            when(erreur.getCodeSysteme()).thenReturn(null);
            when(erreur.getCodeApplicatif()).thenReturn("0000");
            when(erreur.getCodeSortieAppli()).thenReturn(null);
            when(erreur.getLibelleSortieAppli()).thenReturn(null);

            // WHEN / THEN
            assertThatNoException().isThrownBy(() -> service.verifierErreur(erreur));
        }

        // -----------------------------------------------------------------
        // Branche 3 : codeSortieAppli OU libelleSortieAppli présent
        // -----------------------------------------------------------------

        @Test
        @DisplayName("doit lever TransactionException quand codeSortieAppli est non-blank")
        void verifierErreur_codeSortieAppliNonBlank_leveException() {
            // GIVEN
            RetourCICS erreur = mock(RetourCICS.class);
            when(erreur.getCodeSysteme()).thenReturn(null);
            when(erreur.getCodeApplicatif()).thenReturn(null);
            when(erreur.getCodeSortieAppli()).thenReturn("SORT1");
            when(erreur.getLibelleSortieAppli()).thenReturn("Libellé sortie appli");

            // WHEN / THEN
            assertThatThrownBy(() -> service.verifierErreur(erreur))
                    .isInstanceOf(TransactionException.class)
                    .satisfies(ex -> {
                        TransactionException txEx = (TransactionException) ex;
                        assertThat(txEx.getCode()).isEqualTo("SORT1");
                        assertThat(txEx.getMessage()).isEqualTo("Libellé sortie appli");
                        assertThat(txEx.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }

        @Test
        @DisplayName("doit lever TransactionException quand libelleSortieAppli est non-blank (codeSortieAppli blank)")
        void verifierErreur_libelleSortieAppliNonBlankSansCode_leveException() {
            // GIVEN – condition || : codeSortie blank mais libelle présent suffit
            RetourCICS erreur = mock(RetourCICS.class);
            when(erreur.getCodeSysteme()).thenReturn(null);
            when(erreur.getCodeApplicatif()).thenReturn(null);
            when(erreur.getCodeSortieAppli()).thenReturn("");
            when(erreur.getLibelleSortieAppli()).thenReturn("Libellé sans code");

            // WHEN / THEN
            assertThatThrownBy(() -> service.verifierErreur(erreur))
                    .isInstanceOf(TransactionException.class)
                    .satisfies(ex -> {
                        TransactionException txEx = (TransactionException) ex;
                        assertThat(txEx.getCode()).isEqualTo("");        // codeSortieAppli vide
                        assertThat(txEx.getMessage()).isEqualTo("Libellé sans code");
                        assertThat(txEx.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }

        // -----------------------------------------------------------------
        // Branche 4 : aucune erreur → return silencieux
        // -----------------------------------------------------------------

        @Test
        @DisplayName("ne doit PAS lever d'exception quand aucun champ d'erreur n'est renseigné")
        void verifierErreur_aucuneErreur_pasException() {
            // GIVEN
            RetourCICS erreur = retourSansErreur();

            // WHEN / THEN
            assertThatNoException().isThrownBy(() -> service.verifierErreur(erreur));
        }

        @Test
        @DisplayName("ne doit PAS lever d'exception quand tous les champs d'erreur sont blank")
        void verifierErreur_tousChampsBlanks_pasException() {
            // GIVEN
            RetourCICS erreur = mock(RetourCICS.class);
            when(erreur.getCodeSysteme()).thenReturn("");
            when(erreur.getCodeApplicatif()).thenReturn("");
            when(erreur.getCodeSortieAppli()).thenReturn("");
            when(erreur.getLibelleSortieAppli()).thenReturn("");

            // WHEN / THEN
            assertThatNoException().isThrownBy(() -> service.verifierErreur(erreur));
        }

        // -----------------------------------------------------------------
        // Cas limites / priorité des branches
        // -----------------------------------------------------------------

        @Test
        @DisplayName("la branche codeSysteme doit être prioritaire sur codeApplicatif")
        void verifierErreur_codeSystemeEtApplicatifPresents_prioriteAuCodeSysteme() {
            // GIVEN – les deux codes sont en erreur : c'est codeSysteme qui gagne
            RetourCICS erreur = mock(RetourCICS.class);
            when(erreur.getCodeSysteme()).thenReturn("SYS_PRIO");
            when(erreur.getLibelleSysteme()).thenReturn("Libellé système prioritaire");
            when(erreur.getMessageDetaille()).thenReturn("Détail système");
            // codeApplicatif ne doit pas être atteint
            lenient().when(erreur.getCodeApplicatif()).thenReturn("APP_IGNORE");

            // WHEN / THEN
            assertThatThrownBy(() -> service.verifierErreur(erreur))
                    .isInstanceOf(TransactionException.class)
                    .satisfies(ex -> {
                        TransactionException txEx = (TransactionException) ex;
                        assertThat(txEx.getCode()).isEqualTo("Libellé système prioritaire");
                    });

            // Vérification que getCodeApplicatif n'a jamais été appelé
            verify(erreur, never()).getCodeApplicatif();
        }

        @Test
        @DisplayName("le statut HTTP de l'exception doit toujours être INTERNAL_SERVER_ERROR")
        void verifierErreur_statusHttpEstToujoursInternalServerError() {
            // GIVEN
            RetourCICS erreur = mock(RetourCICS.class);
            when(erreur.getCodeSysteme()).thenReturn("ERR");
            when(erreur.getLibelleSysteme()).thenReturn("code");
            when(erreur.getMessageDetaille()).thenReturn("msg");

            // WHEN / THEN
            assertThatThrownBy(() -> service.verifierErreur(erreur))
                    .isInstanceOf(TransactionException.class)
                    .satisfies(ex ->
                            assertThat(((TransactionException) ex).getStatus())
                                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                    );
        }
    }

    // =========================================================================
    // callTransaction
    // =========================================================================

    @Nested
    @DisplayName("callTransaction()")
    class CallTransactionTests {

        @Test
        @DisplayName("doit déléguer à CICS et retourner le CommareaOutput")
        void callTransaction_nominal_delegueACicsEtRetourneOutput() throws Exception {
            // GIVEN
            String transactionName = "KX01";
            Object inputObj = new Object();

            CICSTransaction transaction = mock(CICSTransaction.class);
            CICSInput input = mock(CICSInput.class);
            CommareaOutput expectedOutput = mock(CommareaOutput.class);

            when(cics.getTransaction(transactionName)).thenReturn(transaction);
            when(transaction.createInput()).thenReturn(input);
            when(transaction.execute(any())).thenReturn(expectedOutput);

            // WHEN
            CommareaOutput result = service.callTransaction(inputObj, transactionName);

            // THEN
            assertThat(result).isEqualTo(expectedOutput);
            verify(cics).getTransaction(transactionName);
            verify(transaction).createInput();
            verify(input).set(inputObj);
            verify(transaction).execute(input);
        }

        @Test
        @DisplayName("doit propager l'exception CICS telle quelle")
        void callTransaction_cicsLeveException_propageLException() {
            // GIVEN
            String transactionName = "KX01";
            when(cics.getTransaction(transactionName))
                    .thenThrow(new RuntimeException("Connexion CICS indisponible"));

            // WHEN / THEN
            assertThatThrownBy(() -> service.callTransaction(new Object(), transactionName))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Connexion CICS indisponible");
        }
    }
}
