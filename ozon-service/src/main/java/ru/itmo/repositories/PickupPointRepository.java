package ru.itmo.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.models.PickupPoint;
import ru.itmo.models.projections.PickupPointProjection;

public interface PickupPointRepository extends JpaRepository<PickupPoint, Long> {
    /**
     * Uses Haversine Formula to calculate distance between points
     */
    @Query("""
                SELECT
                    p.id as id,
                    p.address as address,
                    p.city as city,
                    p.lat as lat,
                    p.lng as lng,
                    (
                        6371 * acos(
                            cos(radians(:latIn)) *
                            cos(radians(p.lat)) *
                            cos(radians(p.lng) - radians(:lngIn)) +
                            sin(radians(:latIn)) *
                            sin(radians(p.lat))
                        )
                    ) as distanceKm
                FROM PickupPoint p
                WHERE p.city = :city
                ORDER BY distanceKm
            """)
    Page<PickupPointProjection> findNearest(
            @Param("city") String city,
            @Param("latIn") double lat,
            @Param("lngIn") double lng,
            Pageable pageable
    );

    @Query("""
                SELECT
                    p.id as id,
                    p.address as address,
                    p.city as city,
                    p.lat as lat,
                    p.lng as lng,
                    (
                        6371 * acos(
                            cos(radians(:latIn)) *
                            cos(radians(p.lat)) *
                            cos(radians(p.lng) - radians(:lngIn)) +
                            sin(radians(:latIn)) *
                            sin(radians(p.lat))
                        )
                    ) as distanceKm
                FROM PickupPoint p
                ORDER BY distanceKm
                LIMIT 1
            """)
    PickupPointProjection findNearest(
            @Param("latIn") double lat,
            @Param("lngIn") double lng
    );


}
