package ru.itmo.workers;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.itmo.dto.responses.OrderResponse;
import ru.itmo.security.WorkerSecurityHelper;
import ru.itmo.services.OrderService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetPickupPointOrdersWorker {

    private final ExternalTaskClient client;
    private final OrderService orderService;
    private final WorkerSecurityHelper workerSecurityHelper;

    @PostConstruct
    public void subscribe() {
        client.subscribe("get_pickup_point_orders")
                .lockDuration(60_000)
                .handler(this::execute)
                .open();
    }

    private void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String username = externalTask.getVariable("initiator");
        try {
            log.info("Handling get_pickup_point_orders task {} for user '{}'", externalTask.getId(), username);
            workerSecurityHelper.authenticateAs(username);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            List<OrderResponse> orders = orderService.getOrders(auth);

            externalTaskService.complete(externalTask,
                    Variables.createVariables()
                            .putValue("pickup_point_orders_display", formatOrders(orders)));
        } catch (Exception e) {
            log.error("get_pickup_point_orders failed for user '{}': {}", username, e.getMessage());
            externalTaskService.handleBpmnError(externalTask, "GET_ORDERS_ERROR");
        } finally {
            workerSecurityHelper.clear();
        }
    }

    private String formatOrders(List<OrderResponse> orders) {
        if (orders.isEmpty()) {
            return "Заказов в данном ПВЗ не найдено";
        }
        StringBuilder sb = new StringBuilder();
        for (OrderResponse order : orders) {
            sb.append("Заказ #").append(order.getId())
              .append(" | статус: ").append(order.getOrderStatus())
              .append(" | адрес доставки: ").append(order.getDeliveryAddress());
            if (order.getPickupPoint() != null) {
                sb.append(" | ПВЗ: ").append(order.getPickupPoint().getAddress());
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
