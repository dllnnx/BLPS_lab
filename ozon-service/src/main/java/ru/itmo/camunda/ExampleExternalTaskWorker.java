package ru.itmo.camunda;

import org.camunda.bpm.client.ExternalTaskClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExampleExternalTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(ExampleExternalTaskWorker.class);

    public ExampleExternalTaskWorker(ExternalTaskClient client) {
        client.subscribe("check-address")
                .lockDuration(10_000)
                .handler((task, service) -> {
                    log.info("Handling check-address task {}", task.getId());
                    service.complete(task);
                })
                .open();
    }
}
