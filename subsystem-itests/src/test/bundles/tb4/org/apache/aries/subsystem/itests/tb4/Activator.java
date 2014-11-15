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
package org.apache.aries.subsystem.itests.tb4;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
    private ServiceTracker<String, String> st;

    @Override
    public void start(BundleContext context) throws Exception {
        Filter filter = context.createFilter(
                "(&(objectClass=java.lang.String)(test=testCompositeServiceImports))");
        st = new ServiceTracker<String, String>(context, filter, null);
        st.open();

        String svc = st.waitForService(5000);
        if ("testCompositeServiceImports".equals(svc)) {
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("test", "tb4");
            context.registerService(String.class, "tb4", props);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        st.close();
    }
}
