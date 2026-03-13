package ru.itmo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.models.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
