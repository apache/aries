package org.apache.aries.spifly.dynamic;

import org.apache.aries.spifly.BaseActivator;
import org.apache.aries.spifly.api.SpiFlyConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;

public class DynamicWeavingActivator extends BaseActivator implements BundleActivator {
    private ServiceRegistration<WeavingHook> weavingHookService;

    @Override
    public synchronized void start(BundleContext context) throws Exception {
        WeavingHook wh = new ClientWeavingHook(context, this);
        weavingHookService = context.registerService(WeavingHook.class, wh, null);
        
        super.start(context, SpiFlyConstants.SPI_CONSUMER_HEADER);
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        weavingHookService.unregister();
        
        super.stop(context);
    }
}
