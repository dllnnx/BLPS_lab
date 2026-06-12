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
public class GetAllOrdersWorker {

    private final ExternalTaskClient client;
    private final OrderService orderService;
    private final WorkerSecurityHelper workerSecurityHelper;

    @PostConstruct
    public void subscribe() {
        client.subscribe("get_all_orders")
                .lockDuration(60_000)
                .handler(this::execute)
                .open();
    }

    private void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String username = externalTask.getVariable("initiator");
        try {
            log.info("Handling get_all_orders task {} for user '{}'", externalTask.getId(), username);
            workerSecurityHelper.authenticateAs(username);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            List<OrderResponse> orders = orderService.getOrders(auth);

            externalTaskService.complete(externalTask,
                    Variables.createVariables()
                            .putValue("all_orders_display", formatOrders(orders)));
        } catch (Exception e) {
            log.error("get_all_orders failed for user '{}': {}", username, e.getMessage());
            externalTaskService.handleBpmnError(externalTask, "GET_ALL_ORDERS_ERROR", e.getMessage());
        } finally {
            workerSecurityHelper.clear();
        }
    }

    private String formatOrders(List<OrderResponse> orders) {
        if (orders.isEmpty()) {
            return "Заказов в системе не найдено";
        }
        StringBuilder sb = new StringBuilder();
        for (OrderResponse order : orders) {
            sb.append("Заказ #").append(order.getId())
              .append(" | владелец: ").append(order.getUsername())
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
