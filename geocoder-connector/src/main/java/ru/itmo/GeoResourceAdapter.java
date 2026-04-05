package ru.itmo;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

import javax.transaction.xa.XAResource;

public class GeoResourceAdapter implements ResourceAdapter {
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public void start(BootstrapContext ctx) {}

    @Override
    public void stop() {}

    @Override
    public void endpointActivation(MessageEndpointFactory mef,
                                   ActivationSpec as) {}

    @Override
    public void endpointDeactivation(MessageEndpointFactory mef,
                                     ActivationSpec as) {}

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) {
        return new XAResource[0];
    }
}