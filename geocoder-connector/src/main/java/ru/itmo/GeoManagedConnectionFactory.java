package ru.itmo;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Set;

public class GeoManagedConnectionFactory implements ManagedConnectionFactory, Serializable {
    private PrintWriter writer;

    public GeoManagedConnectionFactory() {
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new GeoConnectionFactoryImpl(cm, this);
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        throw new ResourceException("ConnectionFactory not supported");
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        return new GeoManagedConnection();
    }

    @Override
    public ManagedConnection matchManagedConnections(Set set,
                                                     Subject subject,
                                                     ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {

        if (set == null || set.isEmpty()) {
            return null;
        }
        return (ManagedConnection) set.iterator().next();
    }

    @Override
    public void setLogWriter(PrintWriter printWriter) throws ResourceException {
        writer = printWriter;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return writer;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}