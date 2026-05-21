package br.com.fiap.ClyvoCareAPI.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI clyvoCareOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ClyvoCare API - Cadastros e Contratação")
                        .description("API Java do projeto ClyvoCare (Challenge FIAP SPRINT 1). " +
                                "Responsável pelo cadastro de responsáveis, pets, planos, contratações e tabelas auxiliares.")
                        .version("1.0.0"));
    }
}
