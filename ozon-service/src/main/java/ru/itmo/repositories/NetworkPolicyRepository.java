package ru.itmo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.models.NetworkPolicy;

import java.util.Optional;

public interface NetworkPolicyRepository extends JpaRepository<NetworkPolicy, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<NetworkPolicy> findByName(String name);
}
