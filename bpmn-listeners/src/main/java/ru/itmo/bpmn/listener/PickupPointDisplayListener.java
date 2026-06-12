package ru.itmo.bpmn.listener;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

public class PickupPointDisplayListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        DelegateExecution execution = delegateTask.getExecution();
        Object address = execution.getVariable("delivery_pickup_point_address");
        Object lat = execution.getVariable("delivery_pickup_point_lat");
        Object lng = execution.getVariable("delivery_pickup_point_lng");

        StringBuilder display = new StringBuilder();
        display.append(address == null ? "адрес не определён" : address);
        if (lat != null && lng != null) {
            display.append(" (координаты: ").append(lat).append(", ").append(lng).append(")");
        }
        delegateTask.setVariable("delivery_pickup_point_display", display.toString());
    }
}
