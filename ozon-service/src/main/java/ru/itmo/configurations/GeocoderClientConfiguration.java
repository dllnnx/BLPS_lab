package ru.itmo.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jndi.JndiObjectFactoryBean;
import ru.itmo.GeoConnectionFactory;

@Configuration
public class GeocoderClientConfiguration {
    @Bean
    public GeoConnectionFactory geoConnectionFactory() throws Exception {
        JndiObjectFactoryBean jndi = new JndiObjectFactoryBean();
        jndi.setJndiName("java:/eis/GeoConnectionFactory");
        jndi.setProxyInterface(GeoConnectionFactory.class);
        jndi.afterPropertiesSet();
        return (GeoConnectionFactory) jndi.getObject();
    }
}
