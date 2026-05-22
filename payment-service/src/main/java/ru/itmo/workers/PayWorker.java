package ru.itmo.workers;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;
import ru.itmo.dto.requests.PayRequest;
import ru.itmo.dto.responses.CreatePaymentResponse;
import ru.itmo.services.PaymentService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayWorker {
    private final ExternalTaskClient client;
    private final PaymentService paymentService;
    private String topic = "pay";

    @PostConstruct
    public void subscribe() {
        client.subscribe(topic)
                .lockDuration(1000)
                .handler(this::execute)
                .open();
    }

    private void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        try {
            log.info("Handling {} task {}", topic, externalTask.getId());
            log.info("Task variables: {}", externalTask.getAllVariables());

            String cardNumber = externalTask.getVariable("card_id");
            Integer cardMonth = externalTask.getVariable("card_month");
            Integer cardYear = externalTask.getVariable("card_year");
            String cvc = externalTask.getVariable("cvc");
            UUID paymentId = UUID.fromString(externalTask.getVariable("payment_id"));

            paymentService.pay(new PayRequest(
                    cardNumber,
                    cardMonth,
                    cardYear,
                    cvc,
                    paymentId
            ));

            externalTaskService.complete(externalTask);
        } catch (Exception e) {
            log.error(e.getMessage());
            externalTaskService.handleBpmnError(
                    externalTask,
                    "Возникла ошибка при сохранении оплаты");
        }
    }
}
