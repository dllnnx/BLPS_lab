package ru.itmo.workers;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.itmo.dto.requests.PayRequest;
import ru.itmo.services.PaymentService;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class PayWorker {
    private final ExternalTaskClient client;
    private final PaymentService paymentService;
    private final RestTemplate restTemplate;
    private final String topic = "pay";

    public PayWorker(ExternalTaskClient client, PaymentService paymentService,
                     @Qualifier("camundaRestTemplate") RestTemplate restTemplate) {
        this.client = client;
        this.paymentService = paymentService;
        this.restTemplate = restTemplate;
    }

    @Value("${camunda.client.base-url}")
    private String camundaUrl;

    @PostConstruct
    public void subscribe() {
        client.subscribe(topic)
                .lockDuration(60_000)
                .handler(this::execute)
                .open();
    }

    private void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        try {
            log.info("Handling {} task {}", topic, externalTask.getId());
            log.info("Task variables: {}", externalTask.getAllVariables());

            String cardNumber = externalTask.getVariable("card_id");
            Long orderId = externalTask.getVariable("order_id");
            Long cardMonth = externalTask.getVariable("card_month");
            Long cardYear = externalTask.getVariable("card_year");
            String cvc = externalTask.getVariable("cvc");
            String initiator = externalTask.getVariable("initiator");
            UUID paymentId = UUID.fromString(externalTask.getVariable("payment_id"));

            paymentService.pay(new PayRequest(
                    cardNumber,
                    cardMonth.intValue(),
                    cardYear.intValue(),
                    cvc,
                    paymentId
            ));

            externalTaskService.complete(externalTask);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> requestBody = Map.of(
                    "messageName", "success-payment",
                    "processInstanceId", externalTask.getProcessInstanceId(),
                    "processVariables", Map.of(
                            "order_id", Map.of(
                                    "value", orderId,
                                    "type", "long"
                            ),
                            "initiator", Map.of(
                                    "value", initiator,
                                    "type", "string"
                            )
                    )
            );
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    camundaUrl + "/message",
                    entity,
                    String.class
            );

            log.info("Send message to camunda with response = {}", response);
        } catch (Exception e) {
            log.error(e.getMessage());
            externalTaskService.handleBpmnError(
                    externalTask,
                    "Возникла ошибка при сохранении оплаты");
        }
    }
}
