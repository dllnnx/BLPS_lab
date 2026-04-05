package ru.itmo.clients;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.itmo.GeoConnection;
import ru.itmo.GeoConnectionFactory;
import ru.itmo.dto.middleware.Coordinates;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GeocoderClient {

    private final GeoConnectionFactory connectionFactory;

    public Optional<Coordinates> getCoordinates(String address) {
        try {
            GeoConnection conn = connectionFactory.getConnection();
            Coordinates coordinates = conn.getCoordinates(address);
            return Optional.of(new Coordinates(
                    coordinates.getLatitude(),
                    coordinates.getLongitude()
            ));
        } finally {
        }
    }


}
