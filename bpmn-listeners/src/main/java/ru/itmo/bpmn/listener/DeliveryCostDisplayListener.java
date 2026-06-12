package ru.itmo.bpmn.listener;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

public class DeliveryCostDisplayListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        Object rawCost = delegateTask.getExecution().getVariable("delivery_cost");
        String display = rawCost == null
                ? "стоимость не рассчитана"
                : "Стоимость доставки: " + rawCost + " ₽";
        delegateTask.setVariable("delivery_cost_display", display);
    }
}
