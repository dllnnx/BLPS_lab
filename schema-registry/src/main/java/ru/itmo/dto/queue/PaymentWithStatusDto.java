package ru.itmo.dto.queue;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.itmo.models.PaymentStatus;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentWithStatusDto implements Serializable {

    @NotNull
    private UUID id;

    @NotNull
    private PaymentStatus paymentStatus;
}
