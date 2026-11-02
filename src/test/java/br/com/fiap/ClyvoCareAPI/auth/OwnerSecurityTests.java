package br.com.fiap.ClyvoCareAPI.auth;

import br.com.fiap.ClyvoCareAPI.entity.*;
import br.com.fiap.ClyvoCareAPI.support.ApiIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OwnerSecurityTests extends ApiIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private TokenService tokens;
    @Autowired private JwtEncoder encoder;
    @Autowired private PasswordEncoder passwords;
    @PersistenceContext private EntityManager em;

    private Owner first;
    private Owner second;
    private Owner admin;
    private City city;
    private Species species;
    private Plan plan;
    private Pet firstPet;
    private Pet secondPet;
    private Subscription firstSubscription;
    private Subscription secondSubscription;

    @BeforeEach
    void createAccountsAndResources() {
        var state = State.builder().name("Sao Paulo").uf("SP").build();
        em.persist(state);
        city = City.builder().name("Sao Paulo").state(state).build();
        em.persist(city);
        species = Species.builder().name("Dog").build();
        em.persist(species);
        plan = Plan.builder().name("Basic").monthlyValue(new BigDecimal("100.00")).build();
        em.persist(plan);
        String hash = passwords.encode("password123");
        first = account("first@example.com", "529.982.247-25", "OWNER", hash);
        second = account("second@example.com", "111.444.777-35", "OWNER", hash);
        admin = account("admin@example.com", "123.456.789-09", "ADMIN", hash);
        firstPet = pet(first, "First pet");
        secondPet = pet(second, "Second pet");
        firstSubscription = subscription(firstPet);
        secondSubscription = subscription(secondPet);
        em.flush();
    }

    private Owner account(String email, String cpf, String role, String hash) {
        var owner = Owner.builder().name(role).email(email).cpf(cpf).phone("11999999999")
                .passwordHash(hash).roleName(role).enabled(true).city(city).build();
        em.persist(owner);
        return owner;
    }

    private Pet pet(Owner owner, String name) {
        var pet = Pet.builder().owner(owner).name(name).species(species)
                .birthDate(LocalDate.of(2020, 1, 1)).sex(Sex.MALE).build();
        em.persist(pet);
        return pet;
    }

    private Subscription subscription(Pet pet) {
        var subscription = Subscription.builder().pet(pet).plan(plan).status(SubscriptionStatus.ACTIVE)
                .paymentMethod(PaymentMethod.PIX).contractedValue(new BigDecimal("95.00")).build();
        em.persist(subscription);
        return subscription;
    }

    private String bearer(Owner owner) {
        return "Bearer " + tokens.generateToken(owner.getEmail());
    }

    private String petRequest(Owner owner) {
        return """
                {"name":"Updated pet","birthDate":"2020-01-01","sex":"MALE","ownerId":%d,"speciesId":%d}
                """.formatted(owner.getId(), species.getId());
    }

    private String profileRequest(String cpf, String email) {
        return """
                {"name":"Updated owner","cpf":"%s","email":"%s","password":"password123","phone":"11999999999","cityId":%d}
                """.formatted(cpf, email, city.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/estados", "/cidades"})
    void registrationLocalitiesArePublic(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    void localityDetailsArePublicButWritesRequireAdmin() throws Exception {
        mvc.perform(get("/cidades/" + city.getId())).andExpect(status().isOk());
        mvc.perform(get("/estados/" + city.getState().getId())).andExpect(status().isOk());
        mvc.perform(delete("/cidades/" + city.getId())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/cidades/" + city.getId()).header("Authorization", bearer(first)))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginIssuesOwnerIdentityAndMeReturnsTheSameAccount() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"first@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(get("/auth/me").header("Authorization", bearer(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(first.getId()));
    }

    @Test
    void listsAreScopedToTheAuthenticatedOwner() throws Exception {
        mvc.perform(get("/pets").header("Authorization", bearer(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(firstPet.getId()));
        mvc.perform(get("/pets").param("ownerId", second.getId().toString())
                .header("Authorization", bearer(first))).andExpect(status().isForbidden());
        mvc.perform(get("/contratacoes").header("Authorization", bearer(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(firstSubscription.getId()));
        mvc.perform(get("/contratacoes").param("petId", secondPet.getId().toString())
                .header("Authorization", bearer(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/contratacoes/minhas").header("Authorization", bearer(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(firstSubscription.getId()));
    }

    @Test
    void ownersCannotReadOrMutateAnotherOwnersResources() throws Exception {
        String token = bearer(first);
        mvc.perform(get("/pets/" + secondPet.getId()).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(put("/pets/" + secondPet.getId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(petRequest(first))).andExpect(status().isForbidden());
        mvc.perform(delete("/pets/" + secondPet.getId()).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(post("/pets").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(petRequest(second))).andExpect(status().isForbidden());
        mvc.perform(put("/pets/" + firstPet.getId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(petRequest(second))).andExpect(status().isForbidden());
        mvc.perform(get("/contratacoes/" + secondSubscription.getId()).header("Authorization", token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/contratacoes").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"petId\":%d,\"planId\":%d,\"paymentMethod\":\"PIX\"}".formatted(secondPet.getId(), plan.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/contratacoes/" + secondSubscription.getId() + "/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isForbidden());
        em.clear();
        assertEquals(second.getId(), em.find(Pet.class, secondPet.getId()).getOwner().getId());
        assertEquals(SubscriptionStatus.ACTIVE, em.find(Subscription.class, secondSubscription.getId()).getStatus());
    }

    @Test
    void ownersCannotCancelOrManageSubscriptionStatus() throws Exception {
        String path = "/contratacoes/" + firstSubscription.getId();
        String token = bearer(first);
        mvc.perform(patch(path + "/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(path + "/troca-plano").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"planId\":%d}".formatted(plan.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(patch(path + "/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isForbidden());
        em.flush();
        em.clear();
        assertEquals(SubscriptionStatus.ACTIVE, em.find(Subscription.class, firstSubscription.getId()).getStatus());
        mvc.perform(patch(path + "/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorsCanCancelAnotherOwnersSubscription() throws Exception {
        mvc.perform(patch("/contratacoes/" + firstSubscription.getId() + "/status")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        em.flush();
        em.clear();
        assertEquals(SubscriptionStatus.INACTIVE, em.find(Subscription.class, firstSubscription.getId()).getStatus());
    }

    @Test
    void administratorsRetainCrossOwnerAccess() throws Exception {
        String token = bearer(admin);
        mvc.perform(get("/pets").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/responsaveis/" + second.getId()).header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(patch("/contratacoes/" + secondSubscription.getId() + "/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void ownerProfileAccessAndDuplicateChecksPreserveData() throws Exception {
        String token = bearer(first);
        String originalName = first.getName();
        mvc.perform(get("/responsaveis/" + first.getId()).header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get("/responsaveis/" + second.getId()).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get("/responsaveis").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(put("/responsaveis/" + second.getId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(profileRequest(second.getCpf(), second.getEmail())))
                .andExpect(status().isForbidden());
        mvc.perform(put("/responsaveis/" + first.getId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(profileRequest(second.getCpf(), first.getEmail())))
                .andExpect(status().isConflict());
        mvc.perform(put("/responsaveis/" + first.getId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(profileRequest(first.getCpf(), second.getEmail())))
                .andExpect(status().isConflict());
        em.clear();
        assertEquals(originalName, em.find(Owner.class, first.getId()).getName());
        mvc.perform(put("/responsaveis/" + first.getId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(profileRequest(first.getCpf(), "updated@example.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("updated@example.com"));
        mvc.perform(get("/auth/me").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test
    void deactivationPreservesRecordsAndRejectsExistingTokensAndLogin() throws Exception {
        String token = bearer(first);
        mvc.perform(delete("/responsaveis/" + second.getId()).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(delete("/responsaveis/" + first.getId()).header("Authorization", token)).andExpect(status().isNoContent());
        em.flush();
        em.clear();
        assertFalse(em.find(Owner.class, first.getId()).isEnabled());
        assertNotNull(em.find(Pet.class, firstPet.getId()));
        assertNotNull(em.find(Subscription.class, firstSubscription.getId()));
        mvc.perform(get("/planos").header("Authorization", token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"first@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missingOwner", "wrongOwner", "wrongRole", "unknownRole", "wrongIssuer", "expired", "noExpiry"})
    void invalidIdentityTokensAreRejected(String scenario) throws Exception {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("clyvocare-api").subject(first.getEmail())
                .issuedAt(now.minusSeconds(120)).expiresAt(now.plusSeconds(60))
                .claim("ownerId", first.getId()).claim("role", "OWNER");
        switch (scenario) {
            case "missingOwner" -> claims.claims(values -> values.remove("ownerId"));
            case "wrongOwner" -> claims.claim("ownerId", second.getId());
            case "wrongRole" -> claims.claim("role", "ADMIN");
            case "unknownRole" -> claims.claim("role", "GUEST");
            case "wrongIssuer" -> claims.issuer("other-api");
            case "expired" -> claims.expiresAt(now.minusSeconds(90));
            case "noExpiry" -> claims.claims(values -> values.remove("exp"));
            default -> throw new IllegalArgumentException(scenario);
        }
        String token = encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
        mvc.perform(get("/planos").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void disabledOrDemotedAdministratorsCannotReuseTheirToken() throws Exception {
        String token = bearer(admin);
        admin.setEnabled(false);
        em.flush();
        mvc.perform(get("/responsaveis").header("Authorization", token)).andExpect(status().isUnauthorized());
        admin.setEnabled(true);
        admin.setRoleName("OWNER");
        em.flush();
        mvc.perform(get("/responsaveis").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test
    void modifiedSignatureIsRejected() throws Exception {
        String token = tokens.generateToken(first.getEmail());
        int index = token.lastIndexOf('.') + 5;
        String tampered = token.substring(0, index) + (token.charAt(index) == 'A' ? 'B' : 'A') + token.substring(index + 1);
        mvc.perform(get("/planos").header("Authorization", "Bearer " + tampered)).andExpect(status().isUnauthorized());
    }

    @Test
    void ownersCanManageTheirOwnPetsAndCreatePricedSubscriptions() throws Exception {
        String token = bearer(first);
        mvc.perform(post("/pets").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(petRequest(first))).andExpect(status().isCreated());
        var created = em.createQuery("SELECT p FROM Pet p WHERE p.name = 'Updated pet'", Pet.class).getSingleResult();
        mvc.perform(put("/pets/" + created.getId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(petRequest(first))).andExpect(status().isOk());
        mvc.perform(delete("/pets/" + created.getId()).header("Authorization", token)).andExpect(status().isNoContent());
        em.flush();
        em.clear();
        assertNull(em.find(Pet.class, created.getId()));

        var uncontracted = pet(em.find(Owner.class, first.getId()), "Uncontracted pet");
        em.flush();
        String request = "{\"petId\":%d,\"planId\":%d,\"paymentMethod\":\"PIX\"}"
                .formatted(uncontracted.getId(), plan.getId());
        mvc.perform(post("/contratacoes").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(request)).andExpect(status().isCreated()).andExpect(jsonPath("$.contractedValue").value(95.0));
        mvc.perform(post("/contratacoes").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(request)).andExpect(status().isConflict());
        assertEquals(1L, em.createQuery("SELECT COUNT(s) FROM Subscription s WHERE s.pet.id = :petId", Long.class)
                .setParameter("petId", uncontracted.getId()).getSingleResult());
    }

    @ParameterizedTest
    @CsvSource({"PIX,95.0", "DEBIT_CARD,97.0", "CREDIT_CARD,100.0", "BOLETO,100.0"})
    void simulationPreservesPaymentDiscounts(String method, double expected) throws Exception {
        mvc.perform(post("/contratacoes/simulacao").header("Authorization", bearer(first))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\":%d,\"paymentMethod\":\"%s\"}".formatted(plan.getId(), method)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.finalValue").value(expected));
    }
}
