/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.spifly;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

final class WiringUtils {
    private WiringUtils() {
    }

    static BundleWiring getWiring(Bundle bundle) {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiring != null) {
            return wiring;
        }

        BundleRevision revision = bundle.adapt(BundleRevision.class);
        return revision == null ? null : revision.getWiring();
    }

    static boolean isWiredToExtender(BundleWiring wiring, Bundle mediatorBundle, String extenderName) {
        if (wiring == null) {
            return false;
        }

        for (BundleWire wire : wiring.getRequiredWires(SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE)) {
            Object name = wire.getCapability().getAttributes().get(
                    SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE);
            BundleWiring providerWiring = wire.getProviderWiring();
            if (extenderName.equals(name) && providerWiring != null
                    && providerWiring.getBundle() != null
                    && providerWiring.getBundle().getBundleId() == mediatorBundle.getBundleId()) {
                return true;
            }
        }
        return false;
    }
}
