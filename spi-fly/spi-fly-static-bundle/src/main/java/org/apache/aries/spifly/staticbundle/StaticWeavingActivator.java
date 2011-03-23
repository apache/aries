package org.apache.aries.spifly.staticbundle;

import org.apache.aries.spifly.BaseActivator;
import org.apache.aries.spifly.api.SpiFlyConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class StaticWeavingActivator extends BaseActivator implements BundleActivator {
    @Override
    public synchronized void start(BundleContext context) throws Exception {
        super.start(context, SpiFlyConstants.PROCESSED_SPI_CONSUMER_HEADER);
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        super.stop(context);
    }
}
