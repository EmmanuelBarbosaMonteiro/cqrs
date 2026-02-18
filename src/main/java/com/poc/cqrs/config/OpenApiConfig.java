package com.poc.cqrs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CQRS Order Management - PoC")
                        .version("1.0")
                        .description("""
                                Prova de Conceito de arquitetura **Soft CQRS** com Spring Boot 3, \
                                Spring Data JPA e Materialized Views do PostgreSQL.

                                - **Commands (Escrita)**: Regras de negócio, validações e transições de estado
                                - **Queries (Leitura)**: Leitura genérica direto da Materialized View, sem lógica de negócio
                                - **Queries (JPQL Tipado)**: Consultas otimizadas com projeção para records, sem @Entity de leitura
                                """)
                        .contact(new Contact().name("PoC CQRS")))
                .tags(List.of(
                        new Tag().name("Commands - Escrita")
                                .description("Operações que alteram estado: criar pedido, mudar status, remover item"),
                        new Tag().name("Queries - Leitura")
                                .description("Consultas na Materialized View: listagem paginada com filtros dinâmicos"),
                        new Tag().name("Queries - JPQL Tipado")
                                .description("Consultas com JPQL + projeção para records — demonstra CQRS sem Materialized View, 100% tipado")
                ));
    }
}
