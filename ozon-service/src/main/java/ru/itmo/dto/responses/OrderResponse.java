package ru.itmo.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.itmo.models.OrderStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long id;
    private OrderStatus orderStatus;
    private PickupPointResponse pickupPoint;
    private String deliveryAddress;
    private String username;
}