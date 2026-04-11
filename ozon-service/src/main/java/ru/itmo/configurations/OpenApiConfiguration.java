package ru.itmo.configurations;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfiguration {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/ozon/")))
                .info(new Info()
                        .title("Ozon")
                        .version("1.0")
                        .description("БЛПС лабораторная работа 1"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerScheme",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Токен из POST /api/auth/login; привязан к IP клиента")))
                .addSecurityItem(new SecurityRequirement().addList("bearerScheme"));
    }
}