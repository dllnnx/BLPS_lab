package ru.itmo.producers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.dto.queue.PaymentWithStatusDto;

@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private final JmsTemplate jmsTemplate;

    @Value("${queue.name}")
    private String QUEUE;

    public void sendPaymentStatus(PaymentWithStatusDto message) {
        jmsTemplate.convertAndSend(QUEUE, message);
    }
}
