package ru.itmo.configurations;

import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

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

    @Bean
    public RestTemplate camundaRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
