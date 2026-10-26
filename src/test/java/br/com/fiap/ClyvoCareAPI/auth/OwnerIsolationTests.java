package br.com.fiap.ClyvoCareAPI.auth;

import br.com.fiap.ClyvoCareAPI.entity.*;
import br.com.fiap.ClyvoCareAPI.repository.*;
import br.com.fiap.ClyvoCareAPI.support.TestRsaKeys;
import com.jayway.jsonpath.JsonPath;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:isolation;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.seed.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class OwnerIsolationTests extends TestRsaKeys {
    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OwnerRepository owners;
    @Autowired private PetRepository pets;
    @Autowired private PlanRepository plans;
    @Autowired private SpeciesRepository species;
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private StateRepository states;
    @Autowired private CityRepository cities;

    private Owner owner;
    private Owner other;
    private Pet pet;
    private Pet otherPet;
    private Subscription subscription;
    private Subscription otherSubscription;
    private Plan plan;

    @BeforeEach
    void prepareData() {
        State state = states.save(State.builder().name("Sao Paulo").uf("SP").build());
        City city = cities.save(City.builder().name("Campinas").state(state).build());
        owner = owners.save(Owner.builder().name("Owner").email("owner@test.com").cpf("11111111111")
                .passwordHash("unused").roleName("OWNER").enabled(true).city(city).build());
        other = owners.save(Owner.builder().name("Other").email("other@test.com").cpf("22222222222")
                .passwordHash("unused").roleName("OWNER").enabled(true).city(city).build());
        Species dog = species.save(Species.builder().name("Dog").build());
        pet = pets.save(Pet.builder().name("Rex").owner(owner).species(dog)
                .birthDate(LocalDate.of(2020, 1, 1)).sex(Sex.MALE).build());
        otherPet = pets.save(Pet.builder().name("Bob").owner(other).species(dog)
                .birthDate(LocalDate.of(2020, 1, 1)).sex(Sex.MALE).build());
        plan = plans.save(Plan.builder().name("Basic").monthlyValue(new BigDecimal("100.00")).build());
        subscription = subscriptions.save(Subscription.builder().pet(pet).plan(plan)
                .status(SubscriptionStatus.ACTIVE).paymentMethod(PaymentMethod.PIX)
                .contractedValue(new BigDecimal("95.00")).build());
        otherSubscription = subscriptions.save(Subscription.builder().pet(otherPet).plan(plan)
                .status(SubscriptionStatus.ACTIVE).paymentMethod(PaymentMethod.PIX)
                .contractedValue(new BigDecimal("95.00")).build());
    }

    private RequestPostProcessor asOwner() {
        return jwt().jwt(token -> token.subject(owner.getEmail()).claim("ownerId", owner.getId()))
                .authorities(new SimpleGrantedAuthority("ROLE_OWNER"));
    }

    @Test
    void shouldFilterListsBeforePagination() throws Exception {
        mockMvc.perform(get("/pets").param("size", "1").with(asOwner()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(pet.getId()));
        mockMvc.perform(get("/contratacoes").param("size", "1").with(asOwner()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(subscription.getId()));
        mockMvc.perform(get("/contratacoes").param("petId", otherPet.getId().toString()).with(asOwner()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/pets").param("ownerId", other.getId().toString()).with(asOwner()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReadOwnResources() throws Exception {
        mockMvc.perform(get("/pets/{id}", pet.getId()).with(asOwner())).andExpect(status().isOk());
        mockMvc.perform(get("/contratacoes/{id}", subscription.getId()).with(asOwner())).andExpect(status().isOk());
    }

    @Test
    void shouldRejectOtherResourcesById() throws Exception {
        mockMvc.perform(get("/pets/{id}", otherPet.getId()).with(asOwner())).andExpect(status().isForbidden());
        mockMvc.perform(get("/contratacoes/{id}", otherSubscription.getId()).with(asOwner())).andExpect(status().isForbidden());
        mockMvc.perform(delete("/pets/{id}", otherPet.getId()).with(asOwner())).andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectForgedOwnerAndOwnershipTransfer() throws Exception {
        String payload = petPayload(other.getId());
        mockMvc.perform(post("/pets").with(asOwner()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/pets/{id}", pet.getId()).with(asOwner()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/pets/{id}", otherPet.getId()).with(asOwner())
                        .contentType(MediaType.APPLICATION_JSON).content(petPayload(owner.getId())))
                .andExpect(status().isForbidden());
        assertEquals(owner.getId(), pets.findById(pet.getId()).orElseThrow().getOwner().getId());
    }

    @Test
    void shouldCreateAndUpdateOwnPet() throws Exception {
        mockMvc.perform(post("/pets").with(asOwner()).contentType(MediaType.APPLICATION_JSON)
                        .content(petPayload(owner.getId()))).andExpect(status().isCreated());
        mockMvc.perform(put("/pets/{id}", pet.getId()).with(asOwner()).contentType(MediaType.APPLICATION_JSON)
                        .content(petPayload(owner.getId()))).andExpect(status().isOk());
    }

    @Test
    void shouldRejectSubscriptionForAnotherOwnersPet() throws Exception {
        mockMvc.perform(post("/contratacoes").with(asOwner()).contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionPayload(otherPet.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateOwnSubscriptionWithDiscountAndRejectDuplicate() throws Exception {
        subscription.setStatus(SubscriptionStatus.INACTIVE);
        subscriptions.saveAndFlush(subscription);
        mockMvc.perform(post("/contratacoes").with(asOwner()).contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionPayload(pet.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.contractedValue").value(95.0));
        mockMvc.perform(post("/contratacoes").with(asOwner()).contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionPayload(pet.getId()))).andExpect(status().isConflict());
    }

    @Test
    void shouldCancelOnlyOwnSubscription() throws Exception {
        mockMvc.perform(patch("/contratacoes/{id}/status", otherSubscription.getId()).with(asOwner())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/contratacoes/{id}/status", subscription.getId()).with(asOwner())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        assertEquals(SubscriptionStatus.ACTIVE, subscriptions.findById(otherSubscription.getId()).orElseThrow().getStatus());
    }

    @ParameterizedTest
    @CsvSource({"ACTIVE", "PENDING"})
    void shouldRejectOwnerStatusChangesOtherThanCancellation(String status) throws Exception {
        mockMvc.perform(patch("/contratacoes/{id}/status", subscription.getId()).with(asOwner())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldPreserveAdministratorAccess() throws Exception {
        var admin = jwt().jwt(token -> token.subject("admin@test.com"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mockMvc.perform(get("/pets").with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/contratacoes").with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/pets/{id}", otherPet.getId()).with(admin)).andExpect(status().isOk());
        mockMvc.perform(patch("/contratacoes/{id}/status", otherSubscription.getId()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAuthenticateWithExternalRsaKeys() throws Exception {
        owner.setPasswordHash(passwordEncoder.encode("test-password"));
        owners.saveAndFlush(owner);
        var result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"owner@test.com\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        mockMvc.perform(get("/pets").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/pets/{id}", otherPet.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String petPayload(Long ownerId) {
        return "{\"name\":\"Updated\",\"birthDate\":\"2020-01-01\",\"sex\":\"MALE\",\"ownerId\":"
                + ownerId + ",\"speciesId\":" + pet.getSpecies().getId() + "}";
    }

    private String subscriptionPayload(Long petId) {
        return "{\"petId\":" + petId + ",\"planId\":" + plan.getId() + ",\"paymentMethod\":\"PIX\"}";
    }
}
