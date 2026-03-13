package ru.itmo.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import ru.itmo.dto.requests.CreateOrderRequest;
import ru.itmo.dto.responses.CreateOrderResponse;
import ru.itmo.models.Order;
import ru.itmo.models.OrderStatus;
import ru.itmo.models.PickupPoint;
import ru.itmo.repositories.OrderRepository;
import ru.itmo.repositories.PickupPointRepository;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PickupPointRepository pickupPointRepository;

    public CreateOrderResponse createOrder(@AuthenticationPrincipal User user, CreateOrderRequest request) {

        PickupPoint pickupPoint = pickupPointRepository.findById(request.getPickupPointId())
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Pickup point not found"));


        Order order = orderRepository.save(new Order(
                null,
                null,
                OrderStatus.NEW,
                user.getUsername(),
                pickupPoint,
                request.getDeliveryAddress()
        ));
        return null;
    }

}
