package ru.itmo.configurations;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.itmo.scheduling.PendingPaymentExpiryJob;

@Configuration
public class QuartzConfiguration {

    public static final String PENDING_EXPIRY_JOB_KEY = "pendingPaymentExpiryJob";
    public static final String PENDING_EXPIRY_TRIGGER_KEY = "pendingPaymentExpiryTrigger";

    @Bean
    public JobDetail pendingPaymentExpiryJobDetail() {
        return JobBuilder.newJob(PendingPaymentExpiryJob.class)
                .withIdentity(PENDING_EXPIRY_JOB_KEY)
                .storeDurably()
                .build();
    }

    /**
     * Каждую минуту в 0 секунд (Quartz cron: сек мин час день месяц день-недели).
     */
    @Bean
    public Trigger pendingPaymentExpiryTrigger(JobDetail pendingPaymentExpiryJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(pendingPaymentExpiryJobDetail)
                .withIdentity(PENDING_EXPIRY_TRIGGER_KEY)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/1 * * * ?"))
                .build();
    }
}
