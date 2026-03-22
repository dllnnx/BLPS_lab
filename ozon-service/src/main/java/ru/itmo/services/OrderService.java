package ru.itmo.services;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.clients.PaymentServiceClient;
import ru.itmo.dto.requests.CreateOrderRequest;
import ru.itmo.dto.requests.CreatePaymentRequest;
import ru.itmo.dto.responses.CreateOrderResponse;
import ru.itmo.dto.responses.OrderResponse;
import ru.itmo.models.Order;
import ru.itmo.models.OrderStatus;
import ru.itmo.models.PickupPoint;
import ru.itmo.repositories.OrderRepository;
import ru.itmo.repositories.PickupPointRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PickupPointRepository pickupPointRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final ModelMapper modelMapper;

    @Transactional
    public CreateOrderResponse createOrder(User user, CreateOrderRequest request) {

        PickupPoint pickupPoint = pickupPointRepository.findById(request.getPickupPointId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup point not found"));

        Order order = orderRepository.save(new Order(
                null,
                null,
                OrderStatus.NEW,
                user.getUsername(),
                pickupPoint,
                request.getDeliveryAddress()
        ));
        UUID paymentId;

        try {
            paymentId = paymentServiceClient.createPayment(
                    new CreatePaymentRequest(request.getAmountKopecks())
            ).getPaymentId();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create order, please try later"
            );
        }

        order.setPaymentId(paymentId);
        orderRepository.save(order);

        return new CreateOrderResponse(paymentId);
    }

    public List<OrderResponse> getOrders(User user) {
        List<Order> orders = orderRepository.getOrdersByUsername(user.getUsername());
        return orders.stream().map(o -> modelMapper.map(o, OrderResponse.class)).collect(Collectors.toList());
    }

}
