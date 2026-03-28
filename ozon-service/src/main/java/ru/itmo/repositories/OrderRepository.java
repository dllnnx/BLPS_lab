package ru.itmo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.models.Order;
import ru.itmo.models.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> getOrdersByUsername(String username);

    List<Order> getAllByOrderStatus(OrderStatus orderStatus);

    List<Order> findAllByPickupPoint_IdOrderByIdDesc(Long pickupPointId);

    Optional<Order> findByIdAndUsername(Long id, String username);
}
