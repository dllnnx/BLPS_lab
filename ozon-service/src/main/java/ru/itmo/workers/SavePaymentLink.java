package ru.itmo.workers;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import ru.itmo.services.OrderService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SavePaymentLink {
    private final ExternalTaskClient client;
    private final OrderService orderService;
    private String topic = "save_payment_link";

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

            Long orderId = externalTask.getVariable("order_id");
            UUID paymentId = UUID.fromString(externalTask.getVariable("payment_id"));

            orderService.savePaymentLink(paymentId, orderId);

            externalTaskService.complete(externalTask);
        } catch (Exception e) {
            log.error(e.getMessage());
            externalTaskService.handleBpmnError(
                    externalTask,
                    "Возникла ошибка при сохранении оплаты");
        }
    }
}
