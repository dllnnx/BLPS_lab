package ru.itmo.services;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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

    public CreateOrderResponse createOrder(User user, CreateOrderRequest request) {

        PickupPoint pickupPoint = pickupPointRepository.findById(request.getPickupPointId())
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Pickup point not found"));


        UUID paymentId = paymentServiceClient.createPayment(new CreatePaymentRequest(request.getAmountKopecks())).getPaymentId();
        Order order = orderRepository.save(new Order(
                null,
                paymentId,
                OrderStatus.NEW,
                user.getUsername(),
                pickupPoint,
                request.getDeliveryAddress()
        ));
        return new CreateOrderResponse(paymentId);
    }

    public List<OrderResponse> getOrders(User user) {
        List<Order> orders = orderRepository.getOrdersByUsername(user.getUsername());
        return orders.stream().map(o -> modelMapper.map(o, OrderResponse.class)).collect(Collectors.toList());
    }

}
