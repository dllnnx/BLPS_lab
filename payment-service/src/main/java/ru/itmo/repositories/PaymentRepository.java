package ru.itmo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.models.Payment;
import ru.itmo.models.PaymentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, Instant createdAtBefore);
}
