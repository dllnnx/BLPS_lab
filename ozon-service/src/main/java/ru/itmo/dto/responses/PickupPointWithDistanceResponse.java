package ru.itmo.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PickupPointWithDistanceResponse {

    private Long id;
    private String address;
    private String city;
    private Double lat;
    private Double lng;
    private Double distanceKm;
}