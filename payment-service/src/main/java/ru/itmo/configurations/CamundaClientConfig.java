package ru.itmo.configurations;

import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Configuration
public class CamundaClientConfig {

    @Bean
    public ExternalTaskClient externalTaskClient(
            @Value("${camunda.client.base-url}") String baseUrl,
            @Value("${camunda.admin.username}") String adminUsername,
            @Value("${camunda.admin.password}") String adminPassword) {
        String encoded = Base64.getEncoder()
                .encodeToString((adminUsername + ":" + adminPassword).getBytes());
        return ExternalTaskClient.create()
                .baseUrl(baseUrl)
                .asyncResponseTimeout(10_000)
                .backoffStrategy(new ImmediateBackoff())
                .addInterceptor(ctx -> ctx.addHeader("Authorization", "Basic " + encoded))
                .build();
    }

    private static class ImmediateBackoff implements org.camunda.bpm.client.backoff.BackoffStrategy {
        @Override
        public void reconfigure(java.util.List<org.camunda.bpm.client.task.ExternalTask> fetchedTasks) {}

        @Override
        public long calculateBackoffTime() {
            return 300L;
        }
    }

    @Bean
    public RestTemplate camundaRestTemplate(
            RestTemplateBuilder builder,
            @Value("${camunda.admin.username}") String adminUsername,
            @Value("${camunda.admin.password}") String adminPassword) {
        return builder.basicAuthentication(adminUsername, adminPassword).build();
    }
}
