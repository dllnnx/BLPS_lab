package ru.itmo.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeliveryPriceRequest {
    @NotBlank
    private String address;
}
