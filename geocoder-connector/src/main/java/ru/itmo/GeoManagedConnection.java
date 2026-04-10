package ru.itmo;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.*;

import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

public class GeoManagedConnection implements ManagedConnection {

    private GeoService service = new GeoService();
    private Set<GeoConnectionImpl> handles = new HashSet<>();
    private Set<ConnectionEventListener> listeners = new HashSet<>();

    @Override
    public Object getConnection(Subject subject,
                                ConnectionRequestInfo cxRequestInfo) {
        GeoConnectionImpl conn = new GeoConnectionImpl(this, service);
        handles.add(conn);
        return conn;
    }

    public void closeHandle(GeoConnectionImpl conn) {
        handles.remove(conn);
        ConnectionEvent event =
                new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(conn);

        for (ConnectionEventListener listener : listeners) {
            listener.connectionClosed(event);
        }
    }

    @Override
    public void cleanup() {
        handles.clear();
    }

    @Override
    public void associateConnection(Object o) throws ResourceException {
        if (o instanceof GeoConnectionImpl) {
            GeoConnectionImpl conn = (GeoConnectionImpl) o;
            handles.add(conn);
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        return null;
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        return null;
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter printWriter) throws ResourceException {

    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    @Override
    public void destroy() {}
}