package ru.itmo.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DeliveryPriceResponse {
    private String address;
    private BigDecimal price;
    private PickupPointResponse nearestPickupPoint;
}
