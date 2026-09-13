package br.com.fiap.ClyvoCareAPI.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI clyvoCareOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ClyvoCare API - Cadastros e Contratação")
                        .description("API Java do projeto ClyvoCare (Challenge FIAP SPRINT 1). " +
                                "Responsável pelo cadastro de responsáveis, pets, planos, contratações e tabelas auxiliares.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Insira o token JWT retornado no login (apenas o token, sem o prefixo Bearer)")));
    }
}

