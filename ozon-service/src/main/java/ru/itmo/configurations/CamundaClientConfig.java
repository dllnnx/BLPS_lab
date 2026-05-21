package ru.itmo.configurations;

import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamundaClientConfig {

    @Bean
    public ExternalTaskClient externalTaskClient(
            @Value("${camunda.client.base-url}") String baseUrl) {
        return ExternalTaskClient.create()
                .baseUrl(baseUrl)
                .asyncResponseTimeout(10_000)
                .build();
    }
}
