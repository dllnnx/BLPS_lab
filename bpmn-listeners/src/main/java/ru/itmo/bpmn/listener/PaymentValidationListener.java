package ru.itmo.bpmn.listener;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

import java.time.Year;
import java.time.YearMonth;

public class PaymentValidationListener implements TaskListener {

    private static final String ERROR_CODE = "PAYMENT_INVALID";

    @Override
    public void notify(DelegateTask delegateTask) {
        delegateTask.getExecution().setVariable("payment_error", "");

        String cardId = stringVar(delegateTask, "card_id");
        Long cardMonth = longVar(delegateTask, "card_month");
        Long cardYear = longVar(delegateTask, "card_year");
        String cvc = stringVar(delegateTask, "cvc");

        if (cardId == null || !cardId.matches("\\d{16}") || !luhnValid(cardId)) {
            fail(delegateTask, "Невалидный номер карты: введите 16 цифр, проходящих проверку Луна");
        }
        if (cardMonth == null || cardMonth < 1 || cardMonth > 12) {
            fail(delegateTask, "Невалидный месяц действия карты: укажите число от 1 до 12");
        }
        if (cardYear == null) {
            fail(delegateTask, "Не указан год действия карты");
        }
        int currentYearTwoDigit = Year.now().getValue() % 100;
        int currentMonth = YearMonth.now().getMonthValue();
        if (cardYear < currentYearTwoDigit
                || (cardYear == currentYearTwoDigit && cardMonth < currentMonth)) {
            fail(delegateTask, "Срок действия карты истёк");
        }
        if (cvc == null || !cvc.matches("\\d{3}")) {
            fail(delegateTask, "Невалидный CVC: введите 3 цифры");
        }
    }

    private static void fail(DelegateTask task, String message) {
        task.getExecution().setVariable("payment_error", message);
        throw new BpmnError(ERROR_CODE, message);
    }

    private static String stringVar(DelegateTask task, String name) {
        Object value = task.getVariable(name);
        return value == null ? null : value.toString().trim();
    }

    private static Long longVar(DelegateTask task, String name) {
        Object value = task.getVariable(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean luhnValid(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = number.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
