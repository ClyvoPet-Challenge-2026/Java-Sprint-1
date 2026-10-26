package br.com.fiap.ClyvoCareAPI.auth;

import br.com.fiap.ClyvoCareAPI.controller.CityController;
import br.com.fiap.ClyvoCareAPI.controller.StateController;
import br.com.fiap.ClyvoCareAPI.entity.City;
import br.com.fiap.ClyvoCareAPI.entity.State;
import br.com.fiap.ClyvoCareAPI.support.TestRsaKeys;
import br.com.fiap.ClyvoCareAPI.service.CityService;
import br.com.fiap.ClyvoCareAPI.service.StateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({StateController.class, CityController.class})
@Import({SecurityConfig.class, LocationSecurityTests.CacheConfiguration.class})
class LocationSecurityTests extends TestRsaKeys {

    @TestConfiguration
    static class CacheConfiguration {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StateService stateService;

    @MockitoBean
    private CityService cityService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtEncoder jwtEncoder;

    @Test
    void shouldReadLocationsWithoutAuthentication() throws Exception {
        State state = State.builder().id(1L).name("Sao Paulo").uf("SP").build();
        City city = City.builder().id(2L).name("Campinas").state(state).build();
        when(stateService.findAllStates()).thenReturn(List.of(state));
        when(stateService.findStateById(1L)).thenReturn(state);
        when(cityService.findAllCities()).thenReturn(List.of(city));
        when(cityService.findCityById(2L)).thenReturn(city);

        mockMvc.perform(get("/estados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uf").value("SP"));
        mockMvc.perform(get("/estados/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(get("/cidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state.id").value(1));
        mockMvc.perform(get("/cidades/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Campinas"));
    }

    @ParameterizedTest
    @CsvSource({"POST,/estados", "PUT,/estados/1", "DELETE,/estados/1",
            "POST,/cidades", "PUT,/cidades/2", "DELETE,/cidades/2"})
    void shouldRejectAnonymousWrites(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sao Paulo\",\"uf\":\"SP\",\"stateId\":1}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(stateService, cityService);
    }

    @ParameterizedTest
    @CsvSource({"POST,/estados", "PUT,/estados/1", "DELETE,/estados/1",
            "POST,/cidades", "PUT,/cidades/2", "DELETE,/cidades/2"})
    @WithMockUser(roles = "OWNER")
    void shouldRejectOwnerWrites(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sao Paulo\",\"uf\":\"SP\",\"stateId\":1}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(stateService, cityService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminCreation() throws Exception {
        State state = State.builder().id(1L).name("Sao Paulo").uf("SP").build();
        when(stateService.createState(any())).thenReturn(state);
        when(cityService.createCity(any())).thenReturn(
                City.builder().id(2L).name("Campinas").state(state).build());

        mockMvc.perform(post("/estados").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sao Paulo\",\"uf\":\"SP\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/cidades").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Campinas\",\"stateId\":1}"))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/pets", "/contratacoes", "/responsaveis", "/auth/me"})
    void shouldKeepOtherResourcesAuthenticated(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
}
