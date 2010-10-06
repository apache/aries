/**
 * 
 */
package org.apache.aries.jndi;

import javax.naming.Context;
import javax.naming.NamingException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ContextProvider {
    private ServicePair<?> pair;
    private ServiceReference reference;
    private Context context;
    private BundleContext bc;
    
    public ContextProvider(BundleContext ctx, ServiceReference reference, Context context) {
        bc = ctx;
        this.reference = reference;
        this.context = context;
    }
    
    public boolean isValid() {
        return (reference.getBundle() != null);
    }

    public void close() throws NamingException {
       if (bc != null) bc.ungetService(reference);
       context.close();
    }

    public Context getContext() {
      return context;
    }
    
    @Override
    public void finalize()
    {
      try {
        close();
      } catch (NamingException e) {
        // we are just being nice here, so we ignore this if it happens.
      }
    }
}