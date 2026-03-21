package ru.itmo.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.clients.GeocoderClient;
import ru.itmo.dto.middleware.Coordinates;
import ru.itmo.dto.requests.DeliveryPriceRequest;
import ru.itmo.dto.responses.DeliveryPriceResponse;
import ru.itmo.dto.responses.PickupPointWithDistanceResponse;
import ru.itmo.repositories.PickupPointRepository;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final GeocoderClient geocoderClient;
    private final PickupPointRepository pickupPointRepository;

    private final Double DISTANCE_KM_MAX = 10d;
    private final BigDecimal DISTANCE_COEFFICIENT = BigDecimal.valueOf(100d); // rubles per km

    public DeliveryPriceResponse calculateDeliveryPrice(DeliveryPriceRequest request) {
        Optional<Coordinates> coordinates = geocoderClient.getCoordinates(request.getAddress());
        if (coordinates.isEmpty())
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot find coordinates for that address"
            );

        var nearestPickPoint = pickupPointRepository.findNearest(
                coordinates.get().getLatitude(),
                coordinates.get().getLongitude()
        );
        if (nearestPickPoint.getDistanceKm() >= DISTANCE_KM_MAX)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Too far from nearest pick point"
            );
        return new DeliveryPriceResponse(
                request.getAddress(),
                BigDecimal.valueOf(nearestPickPoint.getDistanceKm()).multiply(DISTANCE_COEFFICIENT),
                new PickupPointWithDistanceResponse(
                        nearestPickPoint.getId(),
                        nearestPickPoint.getAddress(),
                        nearestPickPoint.getCity(),
                        nearestPickPoint.getLat(),
                        nearestPickPoint.getLng(),
                        nearestPickPoint.getDistanceKm()
                )
        );
    }
}
