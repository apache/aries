package org.apache.aries.blueprint.compendium.cm;

import org.osgi.framework.ServiceRegistration;

public final class ServiceUtil {

    private ServiceUtil() {
    }
    
    public static void safeUnregister(ServiceRegistration<?> sreg) {
        if (sreg != null) {
            try {
                sreg.unregister();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

}
