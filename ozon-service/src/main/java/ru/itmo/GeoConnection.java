package ru.itmo;

import ru.itmo.dto.middleware.Coordinates;

/**
 * todo: нужно положить это в schema-registry, у них должен быть одинаковый пакет
 */
public interface GeoConnection {
    Coordinates getCoordinates(String address);
    void close();
}