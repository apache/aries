/**
 * 
 */
package org.apache.aries.jndi;

import javax.naming.Context;
import javax.naming.NamingException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class ContextProvider {
    private final ServiceReference reference;
    private final BundleContext bc;
    
    public ContextProvider(BundleContext ctx, ServiceReference reference) {
        bc = ctx;
        this.reference = reference;
    }
    
    public boolean isValid() {
        return (reference.getBundle() != null);
    }

    public void close() throws NamingException {
       if (bc != null) bc.ungetService(reference);
    }

    public abstract Context getContext() throws NamingException;
}