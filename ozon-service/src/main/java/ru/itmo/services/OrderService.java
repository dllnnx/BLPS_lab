package ru.itmo.services;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import ru.itmo.security.AppUserPrincipal;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PickupPointRepository pickupPointRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final ModelMapper modelMapper;

    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResponse createOrder(AppUserPrincipal user, CreateOrderRequest request) {

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

        return new CreateOrderResponse(order.getId(), paymentId);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(Authentication authentication) {
        AppUserPrincipal user = (AppUserPrincipal) authentication.getPrincipal();
        List<Order> orders;
        if (hasAuthority(authentication, "ORDER_VIEW_ALL")) {
            orders = orderRepository.findAll();
        } else if (hasAuthority(authentication, "ORDER_VIEW_PICKUP_POINT")) {
            Long ppId = user.getPickupPointId();
            if (ppId == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Pickup point is not assigned to this account"
                );
            }
            orders = orderRepository.findAllByPickupPoint_IdOrderByIdDesc(ppId);
        } else {
            orders = orderRepository.getOrdersByUsername(user.getUsername());
        }
        return orders.stream()
                .sorted(Comparator.comparing(Order::getId).reversed())
                .map(o -> modelMapper.map(o, OrderResponse.class))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelOwnOrder(Long orderId, AppUserPrincipal user) {
        Order order = orderRepository.findByIdAndUsername(orderId, user.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getOrderStatus() != OrderStatus.PAYMENT_ERROR) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order can be cancelled only when payment is in error state"
            );
        }
        paymentServiceClient.invalidatePayment(order.getPaymentId());
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markIssuedAtPickup(Long orderId, AppUserPrincipal user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!Objects.equals(order.getPickupPoint().getId(), user.getPickupPointId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order belongs to another pickup point");
        }
        if (order.getOrderStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must be paid before issue");
        }
        order.setOrderStatus(OrderStatus.ISSUED);
        orderRepository.save(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatusByAdmin(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setOrderStatus(newStatus);
        orderRepository.save(order);
    }

    private static boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
