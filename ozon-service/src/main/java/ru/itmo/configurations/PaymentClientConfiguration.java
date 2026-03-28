package ru.itmo.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PaymentClientConfiguration {

    @Bean
    public RestTemplate paymentRestTemplate(
            RestTemplateBuilder builder,
            @Value("${spring.clients.payment-service.username:}") String username,
            @Value("${spring.clients.payment-service.password:}") String password
    ) {
        RestTemplateBuilder b = builder;
        if (username != null && !username.isBlank()) {
            b = b.basicAuthentication(username, password);
        }
        return b.build();
    }
}
