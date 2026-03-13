package ru.itmo.dto.requests;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {

    @Min(0)
    @NotNull
    private Long pickupPointId;

    @Size(min = 1, max = 300)
    private String deliveryAddress;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("10000000.0") // 10M
    private BigDecimal totalPrice;
}
