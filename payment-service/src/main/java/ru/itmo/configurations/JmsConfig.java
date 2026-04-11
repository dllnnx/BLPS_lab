package ru.itmo.configurations;

import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * JMS к брокеру Apache ActiveMQ Classic по протоколу <strong>OpenWire</strong> (Java OpenWire transport).
 * <p>
 * URI {@code tcp://host:port} — TCP + OpenWire; параметры {@code wireFormat.*} в URL настраивают именно OpenWire.
 * </p>
 *
 * @see <a href="https://activemq.apache.org/components/classic/documentation/openwire">OpenWire</a>
 */
@Slf4j
@Configuration
public class JmsConfig {

    @Value("${activemq.broker-url}")
    private String brokerUrl;

    @Value("${activemq.username}")
    private String username;

    @Value("${activemq.password}")
    private String password;

    @Bean(name = "openWireConnectionFactory")
    @Primary
    public ConnectionFactory openWireConnectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        factory.setUserName(username);
        factory.setPassword(password);
        log.info("JMS: ActiveMQConnectionFactory over OpenWire (brokerURL={})", brokerUrl);
        return factory;
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setMessageConverter(messageConverter);
        return jmsTemplate;
    }
}
