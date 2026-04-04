package ru.itmo.clients;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.itmo.dto.requests.CreatePaymentRequest;
import ru.itmo.dto.responses.CreatePaymentResponse;
import ru.itmo.dto.responses.PaymentStatusResponse;

import java.util.UUID;

@Service
public class PaymentServiceClient {

    @Value("${spring.clients.payment-service-url}")
    private String paymentServiceUrl;

    private final RestTemplate paymentRestTemplate;

    public PaymentServiceClient(@Qualifier("paymentRestTemplate") RestTemplate paymentRestTemplate) {
        this.paymentRestTemplate = paymentRestTemplate;
    }

    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        return paymentRestTemplate.postForObject(
                paymentServiceUrl + "/api/payment",
                request,
                CreatePaymentResponse.class
        );
    }

    public PaymentStatusResponse getPaymentStatus(UUID paymentId) {
        return paymentRestTemplate.getForObject(
                paymentServiceUrl + "/api/payment/" + paymentId,
                PaymentStatusResponse.class
        );
    }

    public void invalidatePayment(UUID paymentId) {
        paymentRestTemplate.exchange(
                paymentServiceUrl + "/api/payment/" + paymentId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
        );
    }
}
