package ru.itmo.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.itmo.models.PaymentStatus;

@Data
@AllArgsConstructor
public class PaymentStatusResponse {
    private PaymentStatus status;
}
