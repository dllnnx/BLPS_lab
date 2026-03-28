package ru.itmo.dto.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.itmo.models.OrderStatus;

@Data
public class UpdateOrderStatusRequest {

    @NotNull
    private OrderStatus orderStatus;
}
