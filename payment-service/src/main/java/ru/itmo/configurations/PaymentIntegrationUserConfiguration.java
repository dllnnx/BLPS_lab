package ru.itmo.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class PaymentIntegrationUserConfiguration {

    @Bean
    public UserDetailsService paymentIntegrationUser(
            PasswordEncoder passwordEncoder,
            @Value("${payment.integration.username}") String username,
            @Value("${payment.integration.password}") String rawPassword
    ) {
        UserDetails integration = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .authorities("ROLE_INTEGRATION")
                .build();
        return new InMemoryUserDetailsManager(integration);
    }
}
