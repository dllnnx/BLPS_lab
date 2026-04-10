package ru.itmo;

public interface GeoConnection {
    Coordinates getCoordinates(String address);
    void close();
}