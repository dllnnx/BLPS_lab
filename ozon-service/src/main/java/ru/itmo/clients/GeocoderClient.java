package ru.itmo.clients;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.itmo.Coordinates;
import ru.itmo.GeoConnection;
import ru.itmo.GeoConnectionFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GeocoderClient {

    private final GeoConnectionFactory connectionFactory;

    public Optional<Coordinates> getCoordinates(String address) {
        GeoConnection conn = null;
        try {
            conn = connectionFactory.getConnection();
            Coordinates coordinates = conn.getCoordinates(address);
            return Optional.of(new Coordinates(
                    coordinates.getLatitude(),
                    coordinates.getLongitude()
            ));
        } finally {
            if (conn != null)
                conn.close();
        }
    }


}
