package ru.itmo.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.itmo.dto.requests.NearestPickupPointRequest;
import ru.itmo.dto.responses.PickupPointResponse;
import ru.itmo.models.projections.PickupPointProjection;
import ru.itmo.repositories.PickupPointRepository;

@Service
@RequiredArgsConstructor
public class PickupPointService {
    private final PickupPointRepository pickupPointRepository;

    public Page<PickupPointResponse> findNearest(NearestPickupPointRequest request) {

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        Page<PickupPointProjection> page = pickupPointRepository.findNearest(
                request.getCity(),
                request.getLat(),
                request.getLng(),
                pageable
        );

        return page.map(p -> new PickupPointResponse(
                p.getId(),
                p.getAddress(),
                p.getCity(),
                p.getLat(),
                p.getLng(),
                p.getDistanceKm()
        ));
    }
}
