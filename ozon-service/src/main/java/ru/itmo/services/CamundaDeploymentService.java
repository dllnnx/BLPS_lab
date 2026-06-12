package ru.itmo.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class CamundaDeploymentService {

    private final RestTemplate camundaRestTemplate;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Value("${camunda.client.base-url}")
    private String camundaBaseUrl;

    public CamundaDeploymentService(@Qualifier("camundaRestTemplate") RestTemplate camundaRestTemplate) {
        this.camundaRestTemplate = camundaRestTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void deployResources() {
        log.info("Deploying BPMN and form resources to Camunda...");
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("deployment-name", "ozon-deployment");

            addBpmnFiles(body);
            addHtmlForms(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            var response = camundaRestTemplate.postForEntity(
                    camundaBaseUrl + "/deployment/create",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            Object deploymentId = response.getBody() != null ? response.getBody().get("id") : null;
            if (deploymentId != null) {
                log.info("Camunda deployment created/updated: id={}", deploymentId);
            } else {
                log.info("Camunda deployment unchanged (deploy-changed-only)");
            }
        } catch (Exception e) {
            log.error("Failed to deploy resources to Camunda: {}", e.getMessage(), e);
        }
    }

    private static final String[] BPMN_FILES = {
        "classpath:bpmn/diagram_stepped.bpmn",
        "classpath:bpmn/diagram_ppadmin.bpmn",
        "classpath:bpmn/diagram_superadmin.bpmn"
    };

    private void addBpmnFiles(MultiValueMap<String, Object> body) throws IOException {
        for (String location : BPMN_FILES) {
            Resource res = resolver.getResource(location);
            if (res.exists()) {
                String name = res.getFilename();
                body.add(name, resourceBytes(res, name));
                log.debug("Queued BPMN: {}", name);
            }
        }
    }

    private void addHtmlForms(MultiValueMap<String, Object> body) throws IOException {
        Resource[] forms = resolver.getResources("classpath:bpmn/forms/*.html");
        for (Resource res : forms) {
            String name = "forms/" + res.getFilename();
            body.add(name, resourceBytes(res, name));
            log.debug("Queued form: {}", name);
        }
    }

    private ByteArrayResource resourceBytes(Resource source, String name) throws IOException {
        byte[] bytes = source.getInputStream().readAllBytes();
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return name;
            }
        };
    }
}
