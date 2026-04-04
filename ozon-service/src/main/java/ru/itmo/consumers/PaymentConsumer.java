package ru.itmo.consumers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import ru.itmo.dto.queue.PaymentWithStatusDto;
import ru.itmo.models.Order;
import ru.itmo.models.OrderStatus;
import ru.itmo.models.PaymentStatus;
import ru.itmo.repositories.OrderRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final OrderRepository orderRepository;

    @JmsListener(destination = "${queue.name:payment.status}")
    public void handleMessage(PaymentWithStatusDto message) {
        Order order = orderRepository.findOrderByPaymentId(message.getId()).orElseThrow();
        if (message.getPaymentStatus() == PaymentStatus.FAILED) {
            order.setOrderStatus(OrderStatus.PAYMENT_ERROR);
            orderRepository.save(order);
            log.info("Payment failed: {}", order.getPaymentId());
        } else if (message.getPaymentStatus() == PaymentStatus.COMPLETED) {
            order.setOrderStatus(OrderStatus.PAID);
            orderRepository.save(order);
            log.info("Payment completed: {}", order.getPaymentId());
        } else if (message.getPaymentStatus() == PaymentStatus.INVALID) {
            log.debug("Payment invalidated (order may be cancelled): {}", order.getPaymentId());
        } else {
            log.info("Payment is pending: {}", order.getPaymentId());
        }
    }
}
