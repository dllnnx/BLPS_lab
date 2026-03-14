package ru.itmo.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CreateOrderResponse {
    UUID paymentId;
}
