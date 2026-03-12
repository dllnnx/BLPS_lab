package ru.itmo.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PickupPointResponse {

    private Long id;
    private String address;
    private String city;
    private Double lat;
    private Double lng;
    private Double distanceKm;
}