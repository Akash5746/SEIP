package com.seip.expense.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI expenseServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SEIP Expense Service API")
                        .description("Smart Expense Intelligence Platform - Expense Management Microservice")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SEIP Engineering")
                                .email("engineering@seip.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://seip.com/license")))
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("Local"),
                        new Server().url("http://api-gateway:8080").description("Gateway")
                ));
    }
}
