package ru.itmo.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.itmo.models.AppUser;
import ru.itmo.repositories.AppUserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class CamundaUserSyncService {

    private static final String PROCESS_DEFINITION_KEY = "Process_1qfixpd";

    private static final String GROUP_USER = "user";
    private static final String GROUP_PICKUP_POINT_ADMIN = "pickupPointAdmin";
    private static final String GROUP_APP_ADMIN = "appAdmin";
    private static final String GROUP_CAMUNDA_ADMIN = "camunda-admin";

    private static final int RESOURCE_APPLICATION = 0;
    private static final int RESOURCE_PROCESS_DEFINITION = 6;
    private static final int RESOURCE_TASK = 7;
    private static final int RESOURCE_PROCESS_INSTANCE = 8;

    private static final Map<String, String> APP_ROLE_TO_GROUP = Map.of(
            "USER", GROUP_USER,
            "PICKUP_POINT_ADMIN", GROUP_PICKUP_POINT_ADMIN,
            "ADMIN", GROUP_APP_ADMIN
    );

    private final AppUserRepository appUserRepository;
    private final RestTemplate camundaRestTemplate;
    private final AtomicBoolean syncDone = new AtomicBoolean(false);

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
    public void onReady() {
        attemptSync();
    }

    // Retry every 10s until Camunda accepts the sync (it might be starting after ozon-service).
    @Scheduled(fixedDelay = 10_000L, initialDelay = 10_000L)
    public void retrySync() {
        if (!syncDone.get()) {
            attemptSync();
        }
    }

    private synchronized void attemptSync() {
        if (syncDone.get()) {
            return;
        }
        log.info("Syncing app_users to Camunda identity...");
        try {
            ensureGroups();
            ensureGroupAuthorizations();
            appUserRepository.findAll().forEach(this::syncUser);
            syncDone.set(true);
            log.info("Camunda user sync complete");
        } catch (Exception e) {
            log.warn("Camunda user sync failed (will retry): {}", e.getMessage());
        }
    }

    private void syncUser(AppUser user) {
        String username = user.getUsername();
        if (!userExistsInCamunda(username)) {
            createCamundaUser(username);
            log.info("Created Camunda user '{}' with roles {}", username, user.getRoles());
        } else {
            log.debug("Camunda user '{}' already exists", username);
        }
        for (String role : user.getRoles()) {
            String groupId = APP_ROLE_TO_GROUP.get(role);
            if (groupId != null) {
                addToGroup(username, groupId);
            }
            if ("ADMIN".equals(role)) {
                addToGroup(username, GROUP_CAMUNDA_ADMIN);
            }
        }
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

    private void ensureGroups() {
        createGroupIfMissing(GROUP_USER, "Customers");
        createGroupIfMissing(GROUP_PICKUP_POINT_ADMIN, "Pickup point admins");
        createGroupIfMissing(GROUP_APP_ADMIN, "Application admins");
    }

    private void createGroupIfMissing(String id, String name) {
        try {
            camundaRestTemplate.getForEntity(camundaBaseUrl + "/group/" + id, String.class);
            log.debug("Camunda group '{}' already exists", id);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw e;
            }
            Map<String, Object> body = Map.of("id", id, "name", name, "type", "WORKFLOW");
            camundaRestTemplate.postForEntity(camundaBaseUrl + "/group/create", body, Void.class);
            log.info("Created Camunda group '{}'", id);
        }
    }

    private void ensureGroupAuthorizations() {
        // USER: tasklist + cockpit (so the user can log in at /cockpit and start the process)
        grantGroup(GROUP_USER, RESOURCE_APPLICATION, "tasklist", List.of("ACCESS"));
        grantGroup(GROUP_USER, RESOURCE_APPLICATION, "cockpit", List.of("ACCESS"));
        grantGroup(GROUP_USER, RESOURCE_PROCESS_DEFINITION, PROCESS_DEFINITION_KEY,
                List.of("READ", "READ_INSTANCE", "CREATE_INSTANCE", "READ_TASK"));
        grantGroup(GROUP_USER, RESOURCE_PROCESS_INSTANCE, "*", List.of("CREATE", "READ"));
        grantGroup(GROUP_USER, RESOURCE_TASK, "*", List.of("READ", "UPDATE", "TASK_WORK"));

        // PICKUP_POINT_ADMIN: tasklist + cockpit, view-all on processes
        grantGroup(GROUP_PICKUP_POINT_ADMIN, RESOURCE_APPLICATION, "tasklist", List.of("ACCESS"));
        grantGroup(GROUP_PICKUP_POINT_ADMIN, RESOURCE_APPLICATION, "cockpit", List.of("ACCESS"));
        grantGroup(GROUP_PICKUP_POINT_ADMIN, RESOURCE_PROCESS_DEFINITION, "*",
                List.of("READ", "READ_INSTANCE", "READ_TASK"));
        grantGroup(GROUP_PICKUP_POINT_ADMIN, RESOURCE_PROCESS_INSTANCE, "*", List.of("READ"));
        grantGroup(GROUP_PICKUP_POINT_ADMIN, RESOURCE_TASK, "*", List.of("READ", "UPDATE", "TASK_WORK"));

        // ADMIN: full app access. camunda-admin group membership (added per-user) covers the rest.
        grantGroup(GROUP_APP_ADMIN, RESOURCE_APPLICATION, "*", List.of("ALL"));
    }

    private void grantGroup(String groupId, int resourceType, String resourceId, List<String> permissions) {
        if (authorizationExists(groupId, resourceType, resourceId)) {
            return;
        }
        Map<String, Object> auth = new HashMap<>();
        auth.put("type", 1);
        auth.put("permissions", permissions);
        auth.put("userId", null);
        auth.put("groupId", groupId);
        auth.put("resourceType", resourceType);
        auth.put("resourceId", resourceId);
        try {
            camundaRestTemplate.postForEntity(camundaBaseUrl + "/authorization/create", auth, Map.class);
            log.info("Granted group '{}' permissions {} on resourceType={} resourceId='{}'",
                    groupId, permissions, resourceType, resourceId);
        } catch (Exception e) {
            log.warn("Could not grant group '{}' permissions on resourceType={} resourceId='{}': {}",
                    groupId, resourceType, resourceId, e.getMessage());
        }
    }

    private boolean authorizationExists(String groupId, int resourceType, String resourceId) {
        try {
            var response = camundaRestTemplate.getForEntity(
                    camundaBaseUrl + "/authorization/count?groupIdIn=" + groupId
                            + "&resourceType=" + resourceType
                            + "&resourceId=" + resourceId,
                    Map.class);
            Object count = response.getBody() == null ? null : response.getBody().get("count");
            return count instanceof Number n && n.longValue() > 0;
        } catch (Exception e) {
            log.warn("Could not check authorization for group '{}' on resourceType={} resourceId='{}': {}",
                    groupId, resourceType, resourceId, e.getMessage());
            return false;
        }
    }

    private void addToGroup(String username, String groupId) {
        if (isMember(username, groupId)) {
            return;
        }
        try {
            camundaRestTemplate.put(
                    camundaBaseUrl + "/group/" + groupId + "/members/" + username, null);
            log.info("Added '{}' to group '{}'", username, groupId);
        } catch (Exception e) {
            log.warn("Could not add '{}' to group '{}': {}", username, groupId, e.getMessage());
        }
    }

    private boolean isMember(String username, String groupId) {
        try {
            var response = camundaRestTemplate.getForEntity(
                    camundaBaseUrl + "/group?id=" + groupId + "&member=" + username,
                    List.class);
            List<?> body = response.getBody();
            return body != null && !body.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
