package ru.itmo.dto.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull
    @Min(0)
    private Long amountKopecks;
}
