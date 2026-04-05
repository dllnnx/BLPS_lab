package ru.itmo;

import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;

import javax.naming.NamingException;
import javax.naming.Reference;
import java.io.Serializable;

public class GeoConnectionFactoryImpl implements GeoConnectionFactory, Referenceable, Serializable {

    private ConnectionManager cm;
    private GeoManagedConnectionFactory mcf;
    private Reference reference;

    public GeoConnectionFactoryImpl(ConnectionManager cm,
                                    GeoManagedConnectionFactory mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    @Override
    public GeoConnection getConnection() {
        try {
            return (GeoConnection) cm.allocateConnection(mcf, null);
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    @Override
    public Reference getReference() throws NamingException {
        return reference;
    }
}