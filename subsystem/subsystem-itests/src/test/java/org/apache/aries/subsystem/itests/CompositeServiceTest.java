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
package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.util.tracker.ServiceTracker;

public class CompositeServiceTest extends SubsystemTest {

    @Override
    protected void createApplications() throws Exception {
        createApplication("composite2", "tb4.jar");
    }

    @Test
    public void testCompositeServiceImportExportWildcards() throws Exception {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("test", "testCompositeServiceImports");
        ServiceRegistration<String> reg = bundleContext.registerService(String.class, "testCompositeServiceImports", props);

        Filter filter = bundleContext.createFilter("(&(objectClass=java.lang.String)(test=tb4))");
        ServiceTracker<String, String> st = new ServiceTracker<String, String>(bundleContext, filter, null);
        st.open();

        Subsystem subsystem = installSubsystemFromFile("composite2.esa");
        try {
            assertEquals(Subsystem.State.INSTALLED, subsystem.getState());
            subsystem.start();

            String svc = st.waitForService(5000);
            assertNotNull("The service registered by the bundle inside the composite cannot be found", svc);

            assertEquals(Subsystem.State.ACTIVE, subsystem.getState());
        } finally {
            subsystem.stop();
            uninstallSubsystem(subsystem);
            reg.unregister();
            st.close();
        }
    }
}
