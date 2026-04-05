package ru.itmo;

import java.util.Optional;

public interface GeoConnection {
    Coordinates getCoordinates(String address);
    void close();
}