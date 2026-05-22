package ru.itmo.dto.requests;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class PayRequest {

    @NotBlank
    @Size(max = 17)
    private String cardId;

    @NotNull
    @Min(0)
    @Max(12)
    private Integer month;

    @NotNull
    @Min(0)
    @Max(99)
    private Integer yearTail;

    @NotBlank
    @Size(max = 3)
    private String cvc;

    @NotNull
    private UUID paymentId;
}
