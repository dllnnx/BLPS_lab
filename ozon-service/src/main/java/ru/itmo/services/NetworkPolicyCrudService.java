package ru.itmo.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.dto.network.NetworkPolicyAddressDto;
import ru.itmo.dto.network.NetworkPolicyRequest;
import ru.itmo.dto.network.NetworkPolicyResponse;
import ru.itmo.models.NetworkPolicy;
import ru.itmo.models.NetworkPolicyCidr;
import ru.itmo.repositories.NetworkPolicyRepository;
import ru.itmo.security.CidrUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NetworkPolicyCrudService {

    private final NetworkPolicyRepository networkPolicyRepository;

    @Transactional(readOnly = true)
    public List<NetworkPolicyResponse> findAll() {
        return networkPolicyRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NetworkPolicyResponse findById(Long id) {
        return networkPolicyRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public NetworkPolicyResponse create(NetworkPolicyRequest request) {
        if (networkPolicyRepository.existsByName(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Policy name already exists");
        }
        NetworkPolicy p = new NetworkPolicy();
        applyRequest(p, request);
        return toResponse(networkPolicyRepository.save(p));
    }

    @Transactional
    public NetworkPolicyResponse update(Long id, NetworkPolicyRequest request) {
        NetworkPolicy p = networkPolicyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (networkPolicyRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Policy name already exists");
        }
        applyRequest(p, request);
        return toResponse(networkPolicyRepository.save(p));
    }

    @Transactional
    public void delete(Long id) {
        if (!networkPolicyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        networkPolicyRepository.deleteById(id);
    }

    private void applyRequest(NetworkPolicy p, NetworkPolicyRequest request) {
        p.setName(request.getName().trim());
        p.setDescription(request.getDescription());
        p.getRoleNames().clear();
        p.getRoleNames().addAll(request.getRoleNames());
        p.getAddresses().clear();
        for (NetworkPolicyAddressDto a : request.getAddresses()) {
            CidrUtils.validateIpv4Cidr(a.getAddr());
            NetworkPolicyCidr row = new NetworkPolicyCidr();
            row.setPolicy(p);
            row.setCidr(a.getAddr().trim());
            row.setDescription(a.getDescription());
            p.getAddresses().add(row);
        }
    }

    private NetworkPolicyResponse toResponse(NetworkPolicy p) {
        List<NetworkPolicyAddressDto> addrs = p.getAddresses().stream()
                .map(c -> {
                    NetworkPolicyAddressDto d = new NetworkPolicyAddressDto();
                    d.setAddr(c.getCidr());
                    d.setDescription(c.getDescription());
                    return d;
                })
                .collect(Collectors.toList());
        return new NetworkPolicyResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                addrs,
                p.getRoleNames()
        );
    }
}
