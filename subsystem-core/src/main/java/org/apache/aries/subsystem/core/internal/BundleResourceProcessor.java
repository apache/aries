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
package org.apache.aries.subsystem.core.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.spi.Resource;
import org.apache.aries.subsystem.spi.ResourceProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class BundleResourceProcessor implements ResourceProcessor {

    public Session createSession(BundleContext context) {
        return new BundleSession(context);
    }

    public static class BundleSession implements Session {

        private final BundleContext context;
        private final List<Bundle> installed = new ArrayList<Bundle>();

        public BundleSession(BundleContext context) {
            this.context = context;
        }

        public void process(Resource resource) throws SubsystemException {
            try {
                Bundle bundle = context.installBundle(resource.getLocation(), resource.open());
                installed.add(bundle);
            } catch (SubsystemException e) {
                throw e;
            } catch (Exception e) {
                throw new SubsystemException("Unable to process bundle resource", e);
            }
        }

        public void dropped(Resource resource) throws SubsystemException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void prepare() throws SubsystemException {
        }

        public void commit() {
            installed.clear();
        }

        public void rollback() {
            for (Bundle bundle : installed) {
                try {
                    bundle.uninstall();
                } catch (Exception e) {
                    // Ignore
                }
            }
            installed.clear();
        }
    }

}
