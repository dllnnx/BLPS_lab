package ru.itmo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.itmo.dto.network.NetworkPolicyRequest;
import ru.itmo.dto.network.NetworkPolicyResponse;
import ru.itmo.services.NetworkPolicyCrudService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/network-policies")
@RequiredArgsConstructor
@Tag(name = "Network policies (admin)", description = "CRUD сетевых политик (CIDR + роли)")
public class NetworkPolicyAdminController {

    private final NetworkPolicyCrudService networkPolicyCrudService;

    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_UPDATE_ALL')")
    @Operation(summary = "Список политик")
    public List<NetworkPolicyResponse> list() {
        return networkPolicyCrudService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_UPDATE_ALL')")
    @Operation(summary = "Политика по id")
    public NetworkPolicyResponse get(@PathVariable Long id) {
        return networkPolicyCrudService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORDER_UPDATE_ALL')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать политику")
    public NetworkPolicyResponse create(@Valid @RequestBody NetworkPolicyRequest request) {
        return networkPolicyCrudService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_UPDATE_ALL')")
    @Operation(summary = "Обновить политику")
    public NetworkPolicyResponse update(@PathVariable Long id, @Valid @RequestBody NetworkPolicyRequest request) {
        return networkPolicyCrudService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_UPDATE_ALL')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить политику")
    public void delete(@PathVariable Long id) {
        networkPolicyCrudService.delete(id);
    }
}
