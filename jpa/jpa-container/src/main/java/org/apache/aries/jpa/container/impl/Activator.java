package org.apache.aries.jpa.container.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.apache.aries.jpa.container.parsing.PersistenceDescriptorParser;
import org.apache.aries.jpa.container.parsing.impl.PersistenceDescriptorParserImpl;
import org.apache.aries.jpa.container.tx.impl.OSGiTransactionManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    private PersistenceBundleManager pbm;

	private ServiceRegistration<?> reg;
	
    public void start(BundleContext context) throws Exception {
        PersistenceDescriptorParser parser = new PersistenceDescriptorParserImpl();
        context.registerService(PersistenceDescriptorParser.class.getName(), parser, null);
        pbm = new PersistenceBundleManager(context, parser);
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, "unit");
		reg = context.registerService(PersistenceContextProvider.class.getName(), pbm, properties);
        pbm.open();
    }

    public void stop(BundleContext context) throws Exception {
        reg.unregister();
		pbm.close();
        OSGiTransactionManager otm = OSGiTransactionManager.get();
        if (otm != null)
            otm.destroy();
    }

}
