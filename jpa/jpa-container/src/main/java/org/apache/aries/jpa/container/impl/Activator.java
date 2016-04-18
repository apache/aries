package org.apache.aries.jpa.container.impl;

import org.apache.aries.jpa.container.parsing.PersistenceDescriptorParser;
import org.apache.aries.jpa.container.parsing.impl.PersistenceDescriptorParserImpl;
import org.apache.aries.jpa.container.tx.impl.OSGiTransactionManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
    private PersistenceBundleManager pbm;

    public void start(BundleContext context) throws Exception {
        PersistenceDescriptorParser parser = new PersistenceDescriptorParserImpl();
        context.registerService(PersistenceDescriptorParser.class.getName(), parser, null);
        pbm = new PersistenceBundleManager(context, parser);
        pbm.open();
    }

    public void stop(BundleContext context) throws Exception {
        pbm.close();
        OSGiTransactionManager otm = OSGiTransactionManager.get();
        if (otm != null)
            otm.destroy();
    }

}
