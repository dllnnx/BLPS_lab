package ru.itmo.workers;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;
import ru.itmo.dto.requests.CreateOrderRequest;
import ru.itmo.models.Order;
import ru.itmo.security.WorkerSecurityHelper;
import ru.itmo.services.OrderService;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateOrderWorker {
    private final ExternalTaskClient client;
    private final OrderService orderService;
    private final WorkerSecurityHelper workerSecurityHelper;
    private final String topic = "create_order";

    @PostConstruct
    public void subscribe() {
        client.subscribe(topic)
                .lockDuration(60_000)
                .handler(this::execute)
                .open();
    }

    private void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String username = externalTask.getVariable("initiator");
        try {
            log.info("Handling {} task {} for user '{}'", topic, externalTask.getId(), username);

            workerSecurityHelper.authenticateAs(username);

            String rawAddress = externalTask.getVariable("address_raw");
            Long pickupPointId = externalTask.getVariable("delivery_pickup_point_id");
            String deliveryCostString = externalTask.getVariable("delivery_cost");
            Long deliveryCost = new BigDecimal(deliveryCostString)
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            Order order = orderService.createOnlyOrder(username, new CreateOrderRequest(pickupPointId, rawAddress, deliveryCost));

            externalTaskService.complete(externalTask,
                    Variables.createVariables()
                            .putValue("order_id", order.getId()));
        } catch (Exception e) {
            log.error(e.getMessage());
            externalTaskService.handleBpmnError(
                    externalTask,
                    "Возникла ошибка при создании заказа");
        } finally {
            workerSecurityHelper.clear();
        }
    }
}
