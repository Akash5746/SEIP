package com.seip.fraud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient expenseServiceWebClient(
            @Value("${app.expense-service.url}") String url) {
        return WebClient.builder()
                .baseUrl(url)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    @Bean
    public WebClient mlServiceWebClient(
            @Value("${app.ml-service.url}") String url) {
        return WebClient.builder()
                .baseUrl(url)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
}
