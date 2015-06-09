/*
 * Licensed under the Apache License, Version 2.0 (the "License").
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
package org.apache.aries.subsystem.gogo;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.subsystem.Subsystem;

public class Activator implements BundleActivator {
    private BundleContext bundleContext;

    public void start(BundleContext context) throws Exception {
        bundleContext = context;

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("osgi.command.function", new String[] { "install", "uninstall", "start", "stop", "list" });
        props.put("osgi.command.scope", "subsystem");
        context.registerService(getClass().getName(), this, props);
    }

    public void install(String url) throws IOException {
        Subsystem rootSubsystem = getSubsystem(0);
        System.out.println("Installing subsystem: " + url);
        Subsystem s = rootSubsystem.install(url, new URL(url).openStream());
        System.out.println("Subsystem successfully installed: " + s.getSymbolicName() + "; id: " + s.getSubsystemId());
    }

    public void uninstall(long id) {
        getSubsystem(id).uninstall();
    }

    public void start(long id) {
        getSubsystem(id).start();
    }

    public void stop(long id) {
        getSubsystem(id).stop();
    }

    public void list() throws InvalidSyntaxException {
        Map<Long, String> subsystems = new TreeMap<Long, String>();

        for (ServiceReference<Subsystem> ref : bundleContext.getServiceReferences(Subsystem.class, null)) {
            Subsystem s = bundleContext.getService(ref);
            if (s != null) {
                subsystems.put(s.getSubsystemId(),
                    String.format("%d\t%s\t%s %s", s.getSubsystemId(), s.getState(), s.getSymbolicName(), s.getVersion()));
            }
        }

        for (String entry : subsystems.values()) {
            System.out.println(entry);
        }
    }

    private Subsystem getSubsystem(long id) {
        try {
            for (ServiceReference<Subsystem> ref :
                    bundleContext.getServiceReferences(Subsystem.class, "(subsystem.id=" + id + ")")) {
                Subsystem svc = bundleContext.getService(ref);
                if (svc != null)
                    return svc;
            }
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Unable to find subsystem " + id);
    }

    public void stop(BundleContext context) throws Exception {
    }
}
