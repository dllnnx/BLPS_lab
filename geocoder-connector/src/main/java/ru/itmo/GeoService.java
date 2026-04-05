package ru.itmo;

import java.util.Optional;

public class GeoService {
    public Coordinates getCoordinates(String address) {
        return new Coordinates(12.34d, 56.78d);
    }
}
