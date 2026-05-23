package ru.itmo.workers;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;
import ru.itmo.dto.requests.DeliveryPriceRequest;
import ru.itmo.dto.responses.DeliveryPriceResponse;
import ru.itmo.services.DeliveryService;

import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
@Slf4j
public class CalculateDeliveryWorker {
    private final ExternalTaskClient client;
    private final DeliveryService deliveryService;

    @PostConstruct
    public void subscribe() {
        client.subscribe("calculate_delivery")
                .lockDuration(1000)
                .handler(this::execute)
                .open();
    }

    private void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        try {
            log.info("Handling calculate-delivery task {}", externalTask.getId());
            log.info("Task variables: {}", externalTask.getAllVariables());

            String rawAddress = externalTask.getVariable("address_raw");
            DeliveryPriceResponse deliveryResponse = deliveryService.calculateDeliveryPrice(new DeliveryPriceRequest(rawAddress));

            externalTaskService.complete(externalTask,
                    Variables.createVariables()
                            .putValue("delivery_cost", deliveryResponse.getPrice().setScale(2, RoundingMode.HALF_UP).toString())
                            .putValue("delivery_pickup_point_id", deliveryResponse.getNearestPickupPoint().getId()));
        } catch (Exception e) {
            log.error(e.getMessage());
            externalTaskService.handleBpmnError(
                    externalTask,
                    "Возникла ошибка при расчете стоимости доставки");
        }
    }
}
