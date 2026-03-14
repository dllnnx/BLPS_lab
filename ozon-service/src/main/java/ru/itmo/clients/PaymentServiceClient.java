package ru.itmo.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.itmo.dto.requests.CreatePaymentRequest;
import ru.itmo.dto.responses.CreatePaymentResponse;
import ru.itmo.models.Payment;

import java.util.UUID;

@Service
public class PaymentServiceClient {

    @Value("${spring.clients.payment-service-url}")
    private String paymentServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public CreatePaymentResponse createPayment(CreatePaymentRequest payment) {
        return restTemplate.postForObject(paymentServiceUrl + "/payment", payment, CreatePaymentResponse.class);
    }
}
