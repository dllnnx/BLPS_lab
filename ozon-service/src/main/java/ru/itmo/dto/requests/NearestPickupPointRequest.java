package ru.itmo.dto.requests;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class NearestPickupPointRequest {

    @NotBlank
    private String city;

    @NotNull
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double lat;

    @NotNull
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double lng;

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 10;
}