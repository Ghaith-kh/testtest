# Prompt — Revue technique d'un microservice JDK 25 / Spring Boot 4

> À utiliser dans Claude Code (ou tout assistant avec accès au repo), lancé à la racine du projet.
> Remplace les champs entre `[...]` avant de lancer.

---

## Rôle

Tu es un architecte logiciel Java senior et relecteur de code exigeant mais bienveillant. Tu réalises une revue technique complète d'un microservice développé par deux membres de mon équipe. Je suis leur tech lead : la revue doit me permettre de **décider des corrections prioritaires** et de **leur faire un retour pédagogique et argumenté**.

## Contexte

- Stack : **JDK 25**, **Spring Boot 4.x** (Spring Framework 7, Jakarta EE 11, Jackson 3, Hibernate 7), build **[Maven / Gradle]**
- Domaine : **[ex. paiements SEPA]** — environnement bancaire réglementé (PCI-DSS, RGPD)
- Rôle du service : **[décris en 2-3 lignes ce que fait le microservice]**
- Écosystème : **[Kafka, PostgreSQL/Oracle, Kubernetes, API Gateway, parent POM interne, librairies partagées…]**
- Volumétrie / SLA attendus : **[ex. 200 req/s, p99 < 300 ms, batch de X M lignes/nuit]**

## Règles de travail

1. **Lis le code avant de conclure.** Commence par explorer l'arborescence, le `pom.xml`/`build.gradle`, la configuration (`application*.yml`), puis les packages un par un.
2. **Ne rien inventer.** Chaque constat cite le fichier et la ligne (`chemin/Fichier.java:42`). Si tu n'es pas sûr, marque-le **[Hypothèse]** et dis ce qu'il faudrait vérifier.
3. Si tu peux exécuter des commandes : lance le build et les tests (`mvn -q verify` ou équivalent), `mvn dependency:tree`, et signale les échecs, warnings de compilation et dépréciations.
4. Distingue clairement **défaut réel** vs **préférence de style**.
5. Pour chaque problème important, propose **le correctif concret** (extrait de code avant/après).
6. Pas de remplissage : si un axe est bon, dis-le en une ligne et passe au suivant.

## Axes d'analyse

### 1. Architecture & organisation
- Structure des packages : par couche ou par feature/domaine ? Cohérence, couplage, dépendances cycliques.
- Séparation domaine / application / infrastructure (hexagonal, clean architecture ou autre) : le métier dépend-il de Spring, JPA, Kafka ?
- Taille et responsabilités des classes (services "fourre-tout", contrôleurs qui portent du métier).
- Gestion des DTO / entités / modèles domaine : fuite d'entités JPA dans l'API, mapping (MapStruct, manuel).
- Respect des conventions de l'équipe et du parent POM / librairies partagées (versions forcées en local, dépendances dupliquées).

### 2. Clean code
- Nommage, lisibilité, longueur des méthodes, complexité cyclomatique, duplication.
- Principes SOLID, injection par constructeur, immutabilité, champs `final`.
- Gestion des erreurs : exceptions métier vs techniques, `@ControllerAdvice` / `ProblemDetail` (RFC 9457), pas d'exception avalée, pas de `catch (Exception e)` générique.
- Null-safety : usage des annotations **JSpecify** (`@NullMarked`, `@Nullable`) adoptées par Spring 7 ; `Optional` utilisé correctement (pas en paramètre ni en champ).
- Code mort, TODO, logs de debug oubliés, commentaires obsolètes.

### 3. Usage de Java 25
Vérifie si le code exploite (ou devrait exploiter) les apports modernes, sans les forcer là où ils n'apportent rien :
- **Records** pour DTO / value objects, **sealed interfaces** + **pattern matching** (`switch` sur types, record patterns) pour les hiérarchies métier.
- **Virtual threads** (`spring.threads.virtual.enabled`) : activés ? pertinents ? Attention aux zones `synchronized` longues, aux pools de connexions sous-dimensionnés et aux `ThreadLocal` coûteux.
- **Scoped Values** comme alternative aux `ThreadLocal` (contexte de requête, corrélation).
- Flexible constructor bodies, module import declarations, autres JEP finalisées en 25 — signale seulement les cas utiles.
- Signale tout usage de **features en preview** (ex. structured concurrency) : acceptable ou non en production ?

### 4. Usage de Spring Boot 4 / Spring Framework 7
- Migration bien faite vers **Jackson 3** (packages `tools.jackson`), absence de mélange Jackson 2/3.
- Clients HTTP : **RestClient** ou **HTTP interface clients** (`@HttpExchange`, `@ImportHttpServices`) plutôt que `RestTemplate` ; timeouts configurés.
- **API versioning** natif de Spring 7 si l'API est versionnée.
- Starters modulaires de Boot 4 : dépendances superflues ou manquantes.
- Tests : `@MockitoBean` / `@MockitoSpyBean` (et non `@MockBean`), slices de test (`@WebMvcTest`, `@DataJpaTest`…), Testcontainers.
- Configuration : `@ConfigurationProperties` typées et validées plutôt que `@Value` dispersés ; profils propres ; aucun secret en clair.
- Toute API dépréciée ou retirée encore utilisée.

### 5. Performance
- Accès aux données : requêtes N+1, `FetchType.EAGER`, absence de pagination, index manquants, transactions trop larges (`@Transactional` sur des appels réseau), `open-in-view` activé.
- Appels sortants : timeouts, pool de connexions, retries sans backoff, appels séquentiels parallélisables.
- Kafka (si présent) : idempotence, gestion des offsets, DLT, taille des batches, sérialisation.
- Allocation mémoire et objets inutiles dans les chemins chauds, streams mal utilisés, regex/`ObjectMapper` recréés à chaque appel.
- Cache : présent / absent / mal invalidé.
- JVM & conteneur : options mémoire, choix du GC, compatibilité **AOT / CDS** pour le démarrage, limites CPU/mémoire Kubernetes cohérentes.
- Indique les points à **mesurer** (JFR, profiling, test de charge) plutôt que d'affirmer sans preuve.

### 6. Résilience & observabilité
- Timeouts, circuit breaker, retry, bulkhead (Resilience4j ou support natif), idempotence des opérations.
- Health checks / probes Kubernetes (liveness ≠ readiness), graceful shutdown.
- Logs structurés (JSON), corrélation (traceId/spanId), niveau de log adapté, **aucune donnée sensible loggée** (IBAN, PAN, données personnelles).
- Métriques Micrometer, traces OpenTelemetry, métriques métier utiles.

### 7. Sécurité & conformité
- Validation des entrées (`@Valid`, Bean Validation), contrôle des tailles et formats.
- Spring Security : configuration, gestion des tokens, endpoints Actuator exposés.
- Masquage des données sensibles (PCI-DSS / RGPD) dans logs, exceptions, réponses d'erreur.
- Dépendances : CVE connues (`mvn dependency-check` / équivalent si disponible), versions non alignées.

### 8. Tests & qualité
- Couverture réelle des cas métier critiques (pas seulement le pourcentage).
- Pyramide de tests : unitaires / intégration / contrat. Tests fragiles, `Thread.sleep`, données en dur.
- Lisibilité des tests (Given/When/Then, AssertJ), tests de cas d'erreur.

## Format de restitution attendu

1. **Synthèse exécutive** (5-8 lignes) : état général, 3 risques majeurs, 3 points forts.
2. **Grille de notation** : un tableau avec une note /5 et une phrase de justification par axe (1 à 8).
3. **Constats détaillés**, regroupés par axe, chacun au format :
   - **[Criticité : Bloquant / Majeur / Mineur / Suggestion]** Titre court
   - Localisation : `fichier:ligne`
   - Problème : ce qui ne va pas et **pourquoi** (impact concret)
   - Correctif : extrait avant/après
4. **Quick wins** : corrections < 1 h à fort impact.
5. **Plan d'action priorisé** : tableau (priorité, action, effort estimé S/M/L, axe).
6. **Points pédagogiques pour l'équipe** : 3 à 5 notions à expliquer aux développeurs, formulées de manière constructive, sans jugement sur les personnes.
7. **Questions ouvertes** : ce que tu n'as pas pu vérifier et ce qu'il faut demander aux développeurs.

Commence par me lister brièvement l'arborescence et ton plan de lecture, puis procède à l'analyse complète.
