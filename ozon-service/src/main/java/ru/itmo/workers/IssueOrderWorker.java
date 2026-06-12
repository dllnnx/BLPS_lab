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
import ru.itmo.security.AppUserPrincipal;
import ru.itmo.security.WorkerSecurityHelper;
import ru.itmo.services.OrderService;

@Component
@RequiredArgsConstructor
@Slf4j
public class IssueOrderWorker {

    private final ExternalTaskClient client;
    private final OrderService orderService;
    private final WorkerSecurityHelper workerSecurityHelper;

    @PostConstruct
    public void subscribe() {
        client.subscribe("issue_order")
                .lockDuration(60_000)
                .handler(this::execute)
                .open();
    }

    private void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String username = externalTask.getVariable("initiator");
        try {
            log.info("Handling issue_order task {} for user '{}'", externalTask.getId(), username);
            workerSecurityHelper.authenticateAs(username);

            String orderIdStr = externalTask.getVariable("orderId");
            Long orderId = Long.parseLong(orderIdStr.trim());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();

            orderService.markIssuedAtPickup(orderId, principal);

            externalTaskService.complete(externalTask,
                    Variables.createVariables()
                            .putValue("issue_result", "Заказ #" + orderId + " успешно выдан"));
        } catch (Exception e) {
            log.error("issue_order failed for user '{}': {}", username, e.getMessage());
            externalTaskService.handleBpmnError(externalTask, "ORDER_ISSUE_ERROR", e.getMessage(),
                    Variables.createVariables().putValue("order_issue_error", e.getMessage()));
        } finally {
            workerSecurityHelper.clear();
        }
    }
}
