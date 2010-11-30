/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.subsystem.example.helloIsolation;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class Activator implements BundleActivator {

    private ServiceRegistration sr;
    private BundleTracker bt;
    int addEventCount = 0;
    int removeEventCount = 0;
    int modifyEventCount = 0;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
     * )
     */
    public void start(BundleContext context) throws Exception {
        System.out.println("bundle helloIsolation start");
        
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            System.out.println("HelloIsolationImpl: system manager is not null");
        } else {
            System.out.println("HelloIsolationImpl: system manager is still null");
        }

        
        sr = context.registerService(HelloIsolation.class.getName(),
                new HelloIsolationImpl(), null);
        
        bt = new BundleTracker(context, Bundle.INSTALLED | Bundle.UNINSTALLED | Bundle.ACTIVE, new BundleTrackerCustomizer() {

            public synchronized Object addingBundle(Bundle bundle, BundleEvent event) {
                if (event == null) {
                    System.out.println("HelloIsolation " + bundle.getSymbolicName() + "_" + bundle.getVersion().toString() + " - adding Bundle: " + bundle.getSymbolicName() + " event: null");
                } else {
                    System.out.println("HelloIsolation  " + bundle.getSymbolicName() + "_" + bundle.getVersion().toString() + " - adding Bundle: " + bundle.getSymbolicName() + " event: " + event.getType());
                }
                addEventCount++;
                return bundle;
            }

            public synchronized void modifiedBundle(Bundle bundle, BundleEvent event,
                    Object object) {
                if (event == null) {
                    System.out.println("HelloIsolation " + bundle.getSymbolicName() + "_" + bundle.getVersion().toString() + "  - modifying Bundle: " + bundle.getSymbolicName() + " event: null");
                } else {
                    System.out.println("HelloIsolation " + bundle.getSymbolicName() + "_" + bundle.getVersion().toString() + " - modifying Bundle: " + bundle.getSymbolicName() + " event: " + event.getType());
                }
                modifyEventCount++;
                
            }

            public synchronized void removedBundle(Bundle bundle, BundleEvent event,
                    Object object) {
                if (event == null) {
                    System.out.println("HelloIsolation " + bundle.getSymbolicName() + "_" + bundle.getVersion().toString() + " - removing Bundle: " + bundle.getSymbolicName() + " event: null");
                } else {
                    System.out.println("HelloIsolation " + bundle.getSymbolicName() + "_" + bundle.getVersion().toString() + " - removing Bundle: " + bundle.getSymbolicName() + " event: " + event.getType());
                }
                removeEventCount++;
            }
            
        });
        bt.open();
    }
    
    public int getAddEventCount() {
        return addEventCount;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        System.out.println("bundle helloIsolation stop");
        if (sr != null) {
            sr.unregister();
        }
        
        if (bt != null) {
            bt.close();
        }
    }

}
