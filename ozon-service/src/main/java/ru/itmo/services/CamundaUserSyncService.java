package ru.itmo.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.itmo.models.AppUser;
import ru.itmo.repositories.AppUserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CamundaUserSyncService {

    private final AppUserRepository appUserRepository;
    private final RestTemplate camundaRestTemplate;

    public CamundaUserSyncService(
            AppUserRepository appUserRepository,
            @Qualifier("camundaRestTemplate") RestTemplate camundaRestTemplate
    ) {
        this.appUserRepository = appUserRepository;
        this.camundaRestTemplate = camundaRestTemplate;
    }

    @Value("${camunda.client.base-url}")
    private String camundaBaseUrl;

    @Value("${camunda.sync.default-password}")
    private String syncPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void syncUsers() {
        log.info("Syncing app_users to Camunda identity...");
        try {
            appUserRepository.findAll().forEach(this::syncUser);
            log.info("Camunda user sync complete");
        } catch (Exception e) {
            log.warn("Camunda user sync failed (Camunda might not be ready yet): {}", e.getMessage());
        }
    }

    private void syncUser(AppUser user) {
        String username = user.getUsername();
        if (!userExistsInCamunda(username)) {
            createCamundaUser(username);
            grantAppAccess(username, "cockpit");
            grantAppAccess(username, "tasklist");
            log.info("Created Camunda user '{}' with roles {}", username, user.getRoles());
        } else {
            log.debug("Camunda user '{}' already exists", username);
        }
        addToAdminGroup(username);
    }

    private boolean userExistsInCamunda(String username) {
        try {
            var response = camundaRestTemplate.getForEntity(
                    camundaBaseUrl + "/user/" + username + "/profile", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            return e.getStatusCode() != HttpStatus.NOT_FOUND;
        }
    }

    private void createCamundaUser(String username) {
        Map<String, Object> body = Map.of(
                "profile", Map.of(
                        "id", username,
                        "firstName", username,
                        "lastName", "",
                        "email", ""
                ),
                "credentials", Map.of("password", syncPassword)
        );
        camundaRestTemplate.postForEntity(camundaBaseUrl + "/user/create", body, Void.class);
    }

    private void addToAdminGroup(String username) {
        try {
            camundaRestTemplate.put(
                    camundaBaseUrl + "/group/camunda-admin/members/" + username, null);
        } catch (Exception e) {
            log.warn("Could not add '{}' to camunda-admin group: {}", username, e.getMessage());
        }
    }

    private void grantAppAccess(String username, String app) {
        Map<String, Object> auth = new HashMap<>();
        auth.put("type", 1);
        auth.put("permissions", List.of("ALL"));
        auth.put("userId", username);
        auth.put("groupId", null);
        auth.put("resourceType", 0);
        auth.put("resourceId", app);
        try {
            camundaRestTemplate.postForEntity(camundaBaseUrl + "/authorization/create", auth, Map.class);
        } catch (Exception e) {
            log.warn("Could not grant '{}' access to '{}': {}", username, app, e.getMessage());
        }
    }
}
