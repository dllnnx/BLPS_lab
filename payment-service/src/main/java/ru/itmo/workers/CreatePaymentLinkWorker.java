package ru.itmo.workers;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;
import ru.itmo.dto.responses.CreatePaymentResponse;
import ru.itmo.services.PaymentService;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreatePaymentLinkWorker {
    private final ExternalTaskClient client;
    private final PaymentService paymentService;
    private String topic = "generate-payment-link";

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

            Long deliveryCost = externalTask.getVariable("delivery_cost");

            CreatePaymentResponse response = paymentService.createPayment(deliveryCost);

            externalTaskService.complete(externalTask,
                    Variables.createVariables()
                            .putValue("payment_id", response.getPaymentId().toString()));
        } catch (Exception e) {
            log.error(e.getMessage());
            externalTaskService.handleBpmnError(
                    externalTask,
                    "Возникла ошибка при сохранении оплаты");
        }
    }
}
