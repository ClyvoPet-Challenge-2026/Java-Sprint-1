package br.com.fiap.ClyvoCareAPI.auth;

import br.com.fiap.ClyvoCareAPI.entity.City;
import br.com.fiap.ClyvoCareAPI.entity.Owner;
import br.com.fiap.ClyvoCareAPI.entity.State;
import br.com.fiap.ClyvoCareAPI.repository.CityRepository;
import br.com.fiap.ClyvoCareAPI.repository.OwnerRepository;
import br.com.fiap.ClyvoCareAPI.repository.StateRepository;
import br.com.fiap.ClyvoCareAPI.support.TestRsaKeys;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profile;DB_CLOSE_DELAY=-1",
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
class OwnerProfileTests extends TestRsaKeys {
    @Autowired private MockMvc mockMvc;
    @Autowired private OwnerRepository owners;
    @Autowired private CityRepository cities;
    @Autowired private StateRepository states;
    @Autowired private PasswordEncoder passwordEncoder;
    private Owner owner;
    private Owner other;
    private City city;
    private City newCity;

    @BeforeEach
    void prepareData() {
        State state = states.save(State.builder().name("Sao Paulo").uf("SP").build());
        city = cities.save(City.builder().name("Campinas").state(state).build());
        newCity = cities.save(City.builder().name("Santos").state(state).build());
        owner = owners.save(Owner.builder().name("Owner").cpf("52998224725").email("owner@test.com")
                .passwordHash(passwordEncoder.encode("old-password")).roleName("OWNER").enabled(true)
                .phone("11999999999").city(city).build());
        other = owners.save(Owner.builder().name("Other").cpf("11144477735").email("other@test.com")
                .passwordHash("unused").roleName("OWNER").enabled(true).phone("11988888888").city(city).build());
    }

    private RequestPostProcessor asOwner() {
        return jwt().jwt(token -> token.subject(owner.getEmail()).claim("ownerId", owner.getId()))
                .authorities(new SimpleGrantedAuthority("ROLE_OWNER"));
    }

    private String payload(String cpf, String email, Long cityId) {
        return """
                {"name":"Updated Owner","cpf":"%s","email":"%s","password":"new-password",
                 "phone":"11977777777","cityId":%d}
                """.formatted(cpf, email, cityId);
    }

    @Test
    void shouldReadOwnProfileWithoutExposingPassword() throws Exception {
        mockMvc.perform(get("/responsaveis/{id}", owner.getId()).with(asOwner()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(owner.getId()))
                .andExpect(jsonPath("$.city.state.uf").value("SP"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldUpdateOwnProfileAndHashPassword() throws Exception {
        mockMvc.perform(put("/responsaveis/{id}", owner.getId()).with(asOwner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(owner.getCpf(), owner.getEmail(), newCity.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated Owner"))
                .andExpect(jsonPath("$.city.id").value(newCity.getId()));
        Owner saved = owners.findById(owner.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("new-password", saved.getPasswordHash()));
        assertFalse(passwordEncoder.matches("old-password", saved.getPasswordHash()));
        assertEquals("OWNER", saved.getRoleName());
        assertTrue(saved.isEnabled());
    }

    @Test
    void shouldRejectAccessToAnotherProfileWithoutChangingIt() throws Exception {
        mockMvc.perform(get("/responsaveis/{id}", other.getId()).with(asOwner()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/responsaveis/{id}", other.getId()).with(asOwner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(other.getCpf(), other.getEmail(), newCity.getId())))
                .andExpect(status().isForbidden());
        assertEquals("Other", owners.findById(other.getId()).orElseThrow().getName());
    }

    @Test
    void shouldKeepListingAndDeletionAdministrative() throws Exception {
        mockMvc.perform(get("/responsaveis").with(asOwner())).andExpect(status().isForbidden());
        mockMvc.perform(delete("/responsaveis/{id}", owner.getId()).with(asOwner()))
                .andExpect(status().isForbidden());
        assertTrue(owners.existsById(owner.getId()));
    }

    @Test
    void shouldRejectAnonymousProfileAccess() throws Exception {
        mockMvc.perform(get("/responsaveis/{id}", owner.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/responsaveis/{id}", owner.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(payload(owner.getCpf(), owner.getEmail(), city.getId())))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"11144477735,owner@test.com", "52998224725,other@test.com"})
    void shouldRejectDuplicateCpfOrEmail(String cpf, String email) throws Exception {
        mockMvc.perform(put("/responsaveis/{id}", owner.getId()).with(asOwner())
                        .contentType(MediaType.APPLICATION_JSON).content(payload(cpf, email, city.getId())))
                .andExpect(status().isConflict());
        assertEquals("Owner", owners.findById(owner.getId()).orElseThrow().getName());
        assertEquals("owner@test.com", owners.findById(owner.getId()).orElseThrow().getEmail());
    }

    @ParameterizedTest
    @CsvSource({"11111111111,owner@test.com", "52998224725,invalid-email"})
    void shouldValidateCpfAndEmail(String cpf, String email) throws Exception {
        mockMvc.perform(put("/responsaveis/{id}", owner.getId()).with(asOwner())
                        .contentType(MediaType.APPLICATION_JSON).content(payload(cpf, email, city.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireExistingCity() throws Exception {
        mockMvc.perform(put("/responsaveis/{id}", owner.getId()).with(asOwner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(owner.getCpf(), owner.getEmail(), Long.MAX_VALUE)))
                .andExpect(status().isNotFound());
        assertEquals(city.getId(), owners.findById(owner.getId()).orElseThrow().getCity().getId());
    }

    @Test
    void shouldPreserveAdministratorAccess() throws Exception {
        var admin = jwt().jwt(token -> token.subject("admin@test.com"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mockMvc.perform(get("/responsaveis").with(admin)).andExpect(status().isOk());
        mockMvc.perform(get("/responsaveis/{id}", other.getId()).with(admin)).andExpect(status().isOk());
        mockMvc.perform(put("/responsaveis/{id}", other.getId()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(other.getCpf(), other.getEmail(), newCity.getId())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldNotAcceptPrivilegeEscalationThroughProfilePayload() throws Exception {
        String body = payload(owner.getCpf(), owner.getEmail(), city.getId())
                .replace("\"phone\"", "\"roleName\":\"ADMIN\",\"enabled\":false,\"phone\"");
        mockMvc.perform(put("/responsaveis/{id}", owner.getId()).with(asOwner())
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        assertEquals("OWNER", owners.findById(owner.getId()).orElseThrow().getRoleName());
        assertTrue(owners.findById(owner.getId()).orElseThrow().isEnabled());
    }

    @Test
    void shouldRequireNewLoginAfterEmailChangeEvenIfOldEmailIsReused() throws Exception {
        String token = login("owner@test.com", "old-password");
        mockMvc.perform(put("/responsaveis/{id}", owner.getId()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(owner.getCpf(), "updated@test.com", newCity.getId())))
                .andExpect(status().isOk());
        // Another account takes the old email; the previous JWT must not identify it.
        other.setEmail("owner@test.com");
        owners.saveAndFlush(other);
        for (String path : new String[]{"/auth/me", "/pets", "/contratacoes", "/contratacoes/minhas"}) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
        String refreshed = login("updated@test.com", "new-password");
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + refreshed))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(owner.getId()))
                .andExpect(jsonPath("$.email").value("updated@test.com"));
    }

    @Test
    void shouldRequireNewLoginForTokensWithoutOwnerId() throws Exception {
        var legacy = jwt().jwt(token -> token.subject(owner.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_OWNER"));
        mockMvc.perform(get("/auth/me").with(legacy)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/responsaveis/{id}", owner.getId()).with(legacy))
                .andExpect(status().isUnauthorized());
    }

    private String login(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
