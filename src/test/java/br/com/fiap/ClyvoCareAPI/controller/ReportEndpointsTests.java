package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.auth.TokenService;
import br.com.fiap.ClyvoCareAPI.dto.RevenueReportResponse;
import br.com.fiap.ClyvoCareAPI.dto.SubscriptionReportResponse;
import br.com.fiap.ClyvoCareAPI.entity.Owner;
import br.com.fiap.ClyvoCareAPI.entity.SubscriptionStatus;
import br.com.fiap.ClyvoCareAPI.repository.ReportRepository;
import br.com.fiap.ClyvoCareAPI.support.ApiIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReportEndpointsTests extends ApiIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private TokenService tokens;
    @Autowired private ObjectMapper mapper;
    @PersistenceContext private EntityManager em;
    @MockitoBean private ReportRepository reports;
    private String adminToken;
    private String ownerToken;

    @BeforeEach
    void createAccounts() {
        var admin = Owner.builder().email("report-admin@example.com").roleName("ADMIN").enabled(true).build();
        var owner = Owner.builder().email("report-owner@example.com").roleName("OWNER").enabled(true).build();
        em.persist(admin);
        em.persist(owner);
        em.flush();
        adminToken = "Bearer " + tokens.generateToken(admin.getEmail());
        ownerToken = "Bearer " + tokens.generateToken(owner.getEmail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/relatorios/contratacoes", "/relatorios/receita"})
    void visitorsAndOwnersCannotExecuteReports(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization", ownerToken)).andExpect(status().isForbidden());
        verifyNoInteractions(reports);
    }

    @Test
    void adminReceivesDatabaseJsonAsStructuredItems() throws Exception {
        when(reports.subscriptions(SubscriptionStatus.ACTIVE))
                .thenReturn(new SubscriptionReportResponse(mapper.readTree("[{\"subscription_id\":10}]"), 1));

        mvc.perform(get("/relatorios/contratacoes").param("status", "ACTIVE").header("Authorization", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].subscription_id").value(10))
                .andExpect(jsonPath("$.totalCount").value(1));
        verify(reports).subscriptions(SubscriptionStatus.ACTIVE);
    }

    @Test
    void statusIsOptionalAndEmptyReportsAreSuccessful() throws Exception {
        when(reports.subscriptions(null)).thenReturn(new SubscriptionReportResponse(mapper.createArrayNode(), 0));
        mvc.perform(get("/relatorios/contratacoes").header("Authorization", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void adminReceivesRevenueAndDefaultMinimum() throws Exception {
        var row = new RevenueReportResponse.Row("DETAIL", "Basic", "ACTIVE", 2, new BigDecimal("190.00"));
        when(reports.revenue(BigDecimal.ZERO)).thenReturn(new RevenueReportResponse(List.of(row), new BigDecimal("190.00")));
        mvc.perform(get("/relatorios/receita").header("Authorization", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.grandTotal").value(190.0))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
        verify(reports).revenue(BigDecimal.ZERO);
    }

    @Test
    void minimumIsBoundWithoutRounding() throws Exception {
        when(reports.revenue(new BigDecimal("10.50"))).thenReturn(new RevenueReportResponse(List.of(), BigDecimal.ZERO));
        mvc.perform(get("/relatorios/receita").param("minValue", "10.50").header("Authorization", adminToken))
                .andExpect(status().isOk());
        verify(reports).revenue(new BigDecimal("10.50"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0.001", "100000000", "invalid"})
    void invalidMinimumIsRejectedBeforeCallingOracle(String minValue) throws Exception {
        mvc.perform(get("/relatorios/receita").param("minValue", minValue).header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reports);
    }

    @Test
    void invalidStatusIsRejectedBeforeCallingOracle() throws Exception {
        mvc.perform(get("/relatorios/contratacoes").param("status", "UNKNOWN").header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reports);
    }

    @Test
    void databaseFailuresDoNotExposeOracleDetails() throws Exception {
        when(reports.revenue(BigDecimal.ZERO)).thenThrow(new DataAccessResourceFailureException("private-schema-details"));
        mvc.perform(get("/relatorios/receita").header("Authorization", adminToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Database report is unavailable"))
                .andExpect(content().string(not(containsString("private-schema-details"))));
    }

    @Test
    void failedSubscriptionRecordsAreNotReturnedAsSuccess() throws Exception {
        when(reports.subscriptions(null)).thenThrow(new IllegalStateException("Failed record contains private details"));
        mvc.perform(get("/relatorios/contratacoes").header("Authorization", adminToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(not(containsString("private details"))));
    }
}
