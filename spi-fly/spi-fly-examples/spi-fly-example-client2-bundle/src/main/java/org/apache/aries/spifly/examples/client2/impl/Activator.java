package org.apache.aries.spifly.examples.client2.impl;

import java.util.ServiceLoader;

import org.apache.aries.spifly.mysvc.SPIProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("*** Result from invoking the SPI directly: ");
        ServiceLoader<SPIProvider> ldr = ServiceLoader.load(SPIProvider.class);
        for (SPIProvider spiObject : ldr) {
            System.out.println(spiObject.doit()); // invoke the SPI object
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
