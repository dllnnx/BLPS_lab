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
import ru.itmo.dto.requests.DeliveryPriceRequest;
import ru.itmo.dto.responses.DeliveryPriceResponse;
import ru.itmo.models.Order;
import ru.itmo.services.DeliveryService;
import ru.itmo.services.OrderService;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateOrderWorker {
    private final ExternalTaskClient client;
    private final OrderService orderService;
    private final String topic = "create_order";

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

            String rawAddress = externalTask.getVariable("address_raw");
            Long pickupPointId = externalTask.getVariable("delivery_pickup_point_id");
            String deliveryCostString = externalTask.getVariable("delivery_cost");
            Long deliveryCost = new BigDecimal(deliveryCostString)
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            Order order = orderService.createOnlyOrder(new CreateOrderRequest(pickupPointId, rawAddress, deliveryCost));

            externalTaskService.complete(externalTask,
                    Variables.createVariables()
                            .putValue("order_id", order.getId()));
        } catch (Exception e) {
            log.error(e.getMessage());
            externalTaskService.handleBpmnError(
                    externalTask,
                    "Возникла ошибка при создании заказа");
        }
    }
}
