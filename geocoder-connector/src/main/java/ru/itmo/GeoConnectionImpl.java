package ru.itmo;

public class GeoConnectionImpl implements GeoConnection {

    private final GeoManagedConnection mc;
    private final GeoService service;

    public GeoConnectionImpl(GeoManagedConnection mc, GeoService service) {
        this.mc = mc;
        this.service = service;
    }

    @Override
    public Coordinates getCoordinates(String address) {
        return service.getCoordinates(address);
    }

    @Override
    public void close() {
        mc.closeHandle(this);
    }
}