package ru.itmo.workers;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;
import ru.itmo.models.OrderStatus;
import ru.itmo.security.WorkerSecurityHelper;
import ru.itmo.services.OrderService;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateOrderStatusWorker {

    private final ExternalTaskClient client;
    private final OrderService orderService;
    private final WorkerSecurityHelper workerSecurityHelper;

    @PostConstruct
    public void subscribe() {
        client.subscribe("update_order_status")
                .lockDuration(60_000)
                .handler(this::execute)
                .open();
    }

    private void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String username = externalTask.getVariable("initiator");
        try {
            log.info("Handling update_order_status task {} for user '{}'", externalTask.getId(), username);
            workerSecurityHelper.authenticateAs(username);

            String orderIdStr = externalTask.getVariable("orderId");
            String newStatusStr = externalTask.getVariable("newStatus");

            Long orderId = Long.parseLong(orderIdStr.trim());
            OrderStatus newStatus = OrderStatus.valueOf(newStatusStr.trim());

            orderService.updateOrderStatusByAdmin(orderId, newStatus);

            externalTaskService.complete(externalTask,
                    Variables.createVariables()
                            .putValue("update_result",
                                    "Статус заказа #" + orderId + " успешно изменён на " + newStatus));
        } catch (IllegalArgumentException e) {
            log.error("update_order_status failed for user '{}': invalid input — {}", username, e.getMessage());
            externalTaskService.handleBpmnError(externalTask, "ORDER_UPDATE_ERROR",
                    "Некорректные данные: " + e.getMessage(),
                    Variables.createVariables().putValue("update_order_error", "Некорректные данные: " + e.getMessage()));
        } catch (Exception e) {
            log.error("update_order_status failed for user '{}': {}", username, e.getMessage());
            externalTaskService.handleBpmnError(externalTask, "ORDER_UPDATE_ERROR", e.getMessage(),
                    Variables.createVariables().putValue("update_order_error", e.getMessage()));
        } finally {
            workerSecurityHelper.clear();
        }
    }
}
