package ru.itmo.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.itmo.models.PaymentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {
    private PaymentStatus status;
}
