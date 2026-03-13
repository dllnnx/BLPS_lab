package ru.itmo.dto.responses;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateOrderResponse {
    UUID paymentId;
}
