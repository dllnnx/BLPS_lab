package ru.itmo.models.projections;

public interface PickupPointProjection {

    Long getId();

    String getAddress();

    String getCity();

    Double getLat();

    Double getLng();

    Double getDistanceKm();
}