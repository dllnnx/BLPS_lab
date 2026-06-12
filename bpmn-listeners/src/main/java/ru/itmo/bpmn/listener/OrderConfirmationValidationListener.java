package ru.itmo.bpmn.listener;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

public class OrderConfirmationValidationListener implements TaskListener {

    private static final String ERROR_CODE = "ORDER_INVALID";

    @Override
    public void notify(DelegateTask delegateTask) {
        DelegateExecution execution = delegateTask.getExecution();
        execution.setVariable("order_error", "");

        String address = stringVar(execution, "address_raw");
        if (address == null || address.isBlank()) {
            fail(execution, "Не указан адрес доставки — невозможно оформить заказ");
        }

        Object deliveryCost = execution.getVariable("delivery_cost");
        if (deliveryCost == null || deliveryCost.toString().isBlank()) {
            fail(execution, "Не рассчитана стоимость доставки — нельзя перейти к оплате");
        }

        Boolean toPickupPoint = boolVar(execution, "delivery_to_pickup_point");
        if (Boolean.TRUE.equals(toPickupPoint)) {
            Object pickupPointId = execution.getVariable("delivery_pickup_point_id");
            if (pickupPointId == null) {
                fail(execution, "Доставка в ПВЗ выбрана, но ПВЗ не определён — повторите подбор адреса");
            }
        }

        String initiator = stringVar(execution, "initiator");
        if (initiator == null || initiator.isBlank()) {
            fail(execution, "Не определён инициатор заказа — невозможно оформить");
        }
    }

    private static void fail(DelegateExecution execution, String message) {
        execution.setVariable("order_error", message);
        throw new BpmnError(ERROR_CODE, message);
    }

    private static String stringVar(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        return value == null ? null : value.toString().trim();
    }

    private static Boolean boolVar(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(value.toString());
    }
}
