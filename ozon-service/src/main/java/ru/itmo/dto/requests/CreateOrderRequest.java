package ru.itmo.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @Min(0)
    @NotNull
    @JsonProperty("pickup_point_id")
    private Long pickupPointId;

    @Size(min = 1, max = 300)
    @JsonProperty("delivery_address")
    private String deliveryAddress;

    @NotNull
    @Min(0)
    @JsonProperty("amount_kopecks")
    private Long amountKopecks;
}
