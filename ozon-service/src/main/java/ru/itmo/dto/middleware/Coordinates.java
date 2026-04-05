package ru.itmo.dto.middleware;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Coordinates {
    private Double latitude;
    private Double longitude;
}
