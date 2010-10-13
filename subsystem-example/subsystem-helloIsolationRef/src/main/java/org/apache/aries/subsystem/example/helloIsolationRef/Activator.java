
package org.apache.aries.subsystem.example.helloIsolationRef;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.apache.aries.subsystem.example.helloIsolation.HelloIsolation;

public class Activator implements BundleActivator
{

  /*
   * (non-Javadoc)
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception
  {
    System.out.println("bundle helloIsolationRef start");

    // check to see if we can see helloIsolation service from service registry
    ServiceReference sr = context.getServiceReference(HelloIsolation.class.getName());
    
    if (sr != null) {
        System.out.println("Able to obtain service reference from bundle " 
                + sr.getBundle().getSymbolicName() 
                + "_" + sr.getBundle().getVersion().toString());
        HelloIsolation hi = (HelloIsolation) context.getService(sr);
        hi.hello();
    }
  }

  /*
   * (non-Javadoc)
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception
  {
    System.out.println("bundle helloIsolationRef stop");
  }

}
