package ru.itmo.bpmn.listener;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

public class AddressValidationListener implements TaskListener {

    private static final String ERROR_CODE = "ADDRESS_INVALID";
    private static final int MIN_LENGTH = 5;
    private static final int MIN_LETTERS = 3;

    @Override
    public void notify(DelegateTask delegateTask) {
        DelegateExecution execution = delegateTask.getExecution();
        execution.setVariable("address_error", "");

        Object raw = execution.getVariable("address_raw");
        String address = raw == null ? "" : raw.toString().trim();

        if (address.isEmpty()) {
            fail(execution, "Адрес не указан");
        }
        if (address.length() < MIN_LENGTH) {
            fail(execution, "Адрес слишком короткий, ожидается улица и номер дома");
        }

        int digits = 0;
        int letters = 0;
        for (int i = 0; i < address.length(); i++) {
            char c = address.charAt(i);
            if (Character.isDigit(c)) {
                digits++;
            } else if (Character.isLetter(c)) {
                letters++;
            }
        }

        if (digits == 0) {
            fail(execution, "В адресе нет номера дома — введите полный адрес с домом");
        }
        if (letters < MIN_LETTERS) {
            fail(execution, "В адресе нет названия улицы — введите полный адрес");
        }
    }

    private static void fail(DelegateExecution execution, String message) {
        execution.setVariable("address_error", message);
        throw new BpmnError(ERROR_CODE, message);
    }
}
