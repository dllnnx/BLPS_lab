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

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchPickupPointWorker {
    private final ExternalTaskClient client;
    private final DeliveryService deliveryService;

    @PostConstruct
    public void subscribe() {
        client.subscribe("search-pickup-point")
                .lockDuration(1000)
                .handler(this::execute)
                .open();
    }

    private void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        try {
            log.info("Handling search-pickup-point task {}", externalTask.getId());
            log.info("Task variables: {}", externalTask.getAllVariables());

            String rawAddress = externalTask.getVariable("address-raw");
            DeliveryPriceResponse deliveryResponse = deliveryService.calculateDeliveryPrice(new DeliveryPriceRequest(rawAddress));


            externalTaskService.complete(externalTask,
                    Variables.createVariables()
                            .putValue("delivery_pickup_point_id", deliveryResponse.getNearestPickupPoint().getId())
                            .putValue("delivery_pickup_point_address", deliveryResponse.getNearestPickupPoint().getAddress())
                            .putValue("delivery_pickup_point_lng", deliveryResponse.getNearestPickupPoint().getLng())
                            .putValue("delivery_pickup_point_lat", deliveryResponse.getNearestPickupPoint().getLat())
            );
        } catch (Exception e) {
            log.error(e.getMessage());
            externalTaskService.handleBpmnError(
                    externalTask,
                    "Возникла ошибка при расчете стоимости доставки");
        }
    }
}
