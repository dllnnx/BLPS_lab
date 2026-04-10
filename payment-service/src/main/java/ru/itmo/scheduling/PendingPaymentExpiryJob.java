package ru.itmo.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.QuartzJobBean;
import ru.itmo.services.PaymentService;

/**
 * Quartz: раз в минуту помечаем просроченные PENDING как FAILED.
 */
@Slf4j
@DisallowConcurrentExecution
public class PendingPaymentExpiryJob extends QuartzJobBean {

    @Autowired
    private transient PaymentService paymentService;

    @Override
    protected void executeInternal(@NonNull JobExecutionContext context) {
        int n = paymentService.expireStalePendingPayments();
        if (n > 0) {
            log.info("Pending payment expiry job: marked {} payment(s) as FAILED", n);
        }
    }
}
