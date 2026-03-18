package ru.itmo.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.itmo.clients.PaymentServiceClient;
import ru.itmo.dto.responses.PaymentStatusResponse;
import ru.itmo.models.Order;
import ru.itmo.models.OrderStatus;
import ru.itmo.models.PaymentStatus;
import ru.itmo.repositories.OrderRepository;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRetriever {

    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void retrievePayments() {
        List<Order> unfinishedOrders = orderRepository.getAllByOrderStatus(OrderStatus.NEW);
        log.info("Retrieved {} orders", unfinishedOrders.size());
        for (Order order : unfinishedOrders) {
            PaymentStatusResponse paymentStatusResponse = paymentServiceClient.getPaymentStatus(order.getPaymentId());
            if (paymentStatusResponse.getStatus() == PaymentStatus.FAILED) {
                order.setOrderStatus(OrderStatus.PAYMENT_ERROR);
                orderRepository.save(order);
                log.info("Payment failed: {}", order.getPaymentId());
            } else if (paymentStatusResponse.getStatus() == PaymentStatus.COMPLETED){
                order.setOrderStatus(OrderStatus.PAID);
                orderRepository.save(order);
                log.info("Payment completed: {}", order.getPaymentId());
            } else {
                log.info("Payment is pending: {}", order.getPaymentId());
            }
        }
    }
}
