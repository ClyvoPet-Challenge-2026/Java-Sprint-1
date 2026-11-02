package br.com.fiap.ClyvoCareAPI;

import br.com.fiap.ClyvoCareAPI.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("appservice")
class AppServiceConfigurationTests extends ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocumentationUsesPublicHttpsAddressBehindProxy() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "clyvocare-java.azurewebsites.net")
                        .header("X-Forwarded-Port", "443"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("https://clyvocare-java.azurewebsites.net"));
    }
}
