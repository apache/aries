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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.aries.subsystem.core.ResourceHelper;
import org.apache.aries.subsystem.core.obr.felix.RepositoryAdminRepository;
import org.apache.aries.subsystem.itests.util.RepositoryGenerator;
import org.apache.aries.subsystem.itests.util.Utils;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.ResourceConstants;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemConstants;

public abstract class SubsystemTest extends IntegrationTest {
	protected static class SubsystemEventHandler implements ServiceListener {
		private static class ServiceEventInfo {
			private final ServiceEvent event;
			private final long id;
			private final State state;
			private final String symbolicName;
			private final String type;
			private final Version version;
			
			public ServiceEventInfo(ServiceEvent event) {
				id = (Long)event.getServiceReference().getProperty(SubsystemConstants.SUBSYSTEM_ID_PROPERTY);
				state = (State)event.getServiceReference().getProperty(SubsystemConstants.SUBSYSTEM_STATE_PROPERTY);
				symbolicName = (String)event.getServiceReference().getProperty(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME_PROPERTY);
				type = (String)event.getServiceReference().getProperty(SubsystemConstants.SUBSYSTEM_TYPE_PROPERTY);
				version = (Version)event.getServiceReference().getProperty(SubsystemConstants.SUBSYSTEM_VERSION_PROPERTY);
				this.event = event;
			}
			
			public int getEventType() {
				return event.getType();
			}
			
			public long getId() {
				return id;
			}
			
			public State getState() {
				return state;
			}
			
			public String getSymbolicName() {
				return symbolicName;
			}
			
			public String getType() {
				return type;
			}
			
			public Version getVersion() {
				return version;
			}
		}
		
		private final Map<Long, List<ServiceEventInfo>> subsystemIdToEvents = new HashMap<Long, List<ServiceEventInfo>>();
		
		public void clear() {
			synchronized (subsystemIdToEvents) {
				subsystemIdToEvents.clear();
			}
		}
		
		public ServiceEventInfo poll(long subsystemId) throws InterruptedException {
			return poll(subsystemId, 0);
		}
		
		public ServiceEventInfo poll(long subsystemId, long timeout) throws InterruptedException {
			List<ServiceEventInfo> events;
			synchronized (subsystemIdToEvents) {
				events = subsystemIdToEvents.get(subsystemId);
				if (events == null) {
					events = new ArrayList<ServiceEventInfo>();
					subsystemIdToEvents.put(subsystemId, events);
				}
			}
			synchronized (events) {
				if (events.isEmpty()) {
					events.wait(timeout);
					if (events.isEmpty()) {
						return null;
					}
				}
				return events.remove(0);
			}
		}
		
		public void serviceChanged(ServiceEvent event) {
			Long subsystemId = (Long)event.getServiceReference().getProperty(SubsystemConstants.SUBSYSTEM_ID_PROPERTY);
			synchronized (subsystemIdToEvents) {
				List<ServiceEventInfo> events = subsystemIdToEvents.get(subsystemId);
				if (events == null) {
					events = new ArrayList<ServiceEventInfo>();
					subsystemIdToEvents.put(subsystemId, events);
				}
				synchronized (events) {
					events.add(new ServiceEventInfo(event));
					events.notify();
				}
			}
		}
		
		public int size() {
			synchronized (subsystemIdToEvents) {
				return subsystemIdToEvents.size();
			}
		}
	}
	
	@org.ops4j.pax.exam.junit.Configuration
	public static Option[] configuration() {
		Option[] options = options(
				// this is how you set the default log level when using pax
				// logging (logProfile)
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
				systemProperty("org.osgi.framework.bsnversion").value("multiple"),
				// Log
				mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
				mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
				// Felix Config Admin
//				mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
				// Felix mvn url handler
				mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),
				// Bundles
				mavenBundle("org.eclipse.osgi", "org.eclipse.osgi.services").version("3.8.0-SNAPSHOT"),
				mavenBundle("org.eclipse.equinox", "org.eclipse.equinox.region").version("3.8.0-SNAPSHOT"),
				mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
				mavenBundle("org.apache.aries", "org.apache.aries.util").version("0.5-SNAPSHOT"),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
				mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository"),
				mavenBundle("org.eclipse.equinox", "org.eclipse.equinox.coordinator").version("3.8.0-SNAPSHOT"),
				mavenBundle("org.eclipse.equinox", "org.eclipse.equinox.event").version("3.8.0-SNAPSHOT"),
				mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.api"),
				mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.core"),
//				org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
				PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),
				equinox().version("3.8.0-SNAPSHOT"));
		options = updateOptions(options);
		return options;
	}
	
	protected final SubsystemEventHandler subsystemEvents = new SubsystemEventHandler();
	
	private Collection<ServiceRegistration<?>> serviceRegistrations = new ArrayList<ServiceRegistration<?>>();
	
	public void setUp() {
		super.setUp();
		new RepositoryGenerator(bundleContext).generateOBR();
		serviceRegistrations.add(bundleContext.registerService(Repository.class, new RepositoryAdminRepository(getOsgiService(RepositoryAdmin.class)), null));
		try {
			bundleContext.getBundle(0).getBundleContext().addServiceListener(subsystemEvents, '(' + Constants.OBJECTCLASS + '=' + Subsystem.class.getName() + ')');
		}
		catch (InvalidSyntaxException e) {
			fail("Invalid filter: " + e.getMessage());
		}
		assertSubsystemNotNull(getRootSubsystem());
	}
	
	public void tearDown() {
		bundleContext.removeServiceListener(subsystemEvents);
		for (ServiceRegistration<?> registration : serviceRegistrations)
			Utils.unregisterQuietly(registration);
		serviceRegistrations.clear();
		super.tearDown();
	}
	
	protected void assertChild(Subsystem parent, Subsystem child) {
		Collection<Subsystem> children = new ArrayList<Subsystem>(1);
		children.add(child);
		assertChildren(parent, children);
	}
	
	protected void assertChildren(int size, Subsystem subsystem) {
		assertEquals("Wrong number of children", size, subsystem.getChildren().size());
	}
	
	protected void assertChildren(Subsystem parent, Collection<Subsystem> children) {
		assertTrue("Parent did not contain all children", parent.getChildren().containsAll(children));
	}
	
	protected void assertConstituent(Subsystem subsystem, String symbolicName, Version version, String type) {
		for (Resource resource : subsystem.getConstituents()) {
			if (symbolicName.equals(ResourceHelper.getSymbolicNameAttribute(resource))) {
				if (version != null)
					assertVersion(version, ResourceHelper.getVersionAttribute(resource));
				if (type != null)
					assertEquals("Wrong type", type, ResourceHelper.getTypeAttribute(resource));
				return;
			}
		}
		Assert.fail("Constituent not found: " + symbolicName);
	}
	
	protected void assertConstituents(int size, Subsystem subsystem) {
		assertEquals("Wrong number of constituents", size, subsystem.getConstituents().size());
	}
	
 	protected void assertDirectory(Subsystem subsystem) {
 		Bundle bundle = getSubsystemCoreBundle();
 		File file = bundle.getDataFile("subsystem" + subsystem.getSubsystemId());
 		assertNotNull("Subsystem data file was null", file);
 		assertTrue("Subsystem data file does not exist", file.exists());
 	}
 	
 	protected void assertNotDirectory(Subsystem subsystem) {
 		Bundle bundle = getSubsystemCoreBundle();
 		File file = bundle.getDataFile("subsystem" + subsystem.getSubsystemId());
 		assertNotNull("Subsystem data file was null", file);
 		assertFalse("Subsystem data file exists", file.exists());
 	}
 	
 	protected void assertEvent(Subsystem subsystem, Subsystem.State state) throws InterruptedException {
 		assertEvent(subsystem, state, 0);
 	}
 	
 	protected void assertEvent(Subsystem subsystem, Subsystem.State state, long timeout) throws InterruptedException {
 		assertEvent(subsystem, state, subsystemEvents.poll(subsystem.getSubsystemId(), timeout));
 	}
 	protected void assertEvent(Subsystem subsystem, Subsystem.State state, SubsystemEventHandler.ServiceEventInfo event) {
 		if (State.INSTALLING.equals(state))
			assertEvent(subsystem, state, event, ServiceEvent.REGISTERED);
 		else
 			assertEvent(subsystem, state, event, ServiceEvent.MODIFIED);
 	}
	
	protected void assertEvent(Subsystem subsystem, Subsystem.State state, SubsystemEventHandler.ServiceEventInfo event, int type) {
		// TODO Could accept a ServiceRegistration as an argument and verify it against the one in the event.
		assertEquals("Wrong ID", subsystem.getSubsystemId(), event.getId());
		assertEquals("Wrong symbolic name", subsystem.getSymbolicName(), event.getSymbolicName());
		assertEquals("Wrong version", subsystem.getVersion(), event.getVersion());
		assertEquals("Wrong type", subsystem.getType(), event.getType());
		assertEquals("Wrong state", state, event.getState());
		assertEquals("Wrong event type", type, event.getEventType());
	}
	
	protected void assertId(Subsystem subsystem) {
		assertId(subsystem.getSubsystemId());
	}
	
	protected void assertId(Long id) {
		assertTrue("Subsystem ID was not a positive integer: " + id, id > 0);
	}
	
	protected void assertLastId(long id) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Subsystem root = getRootSubsystem();
		Field lastId = root.getClass().getDeclaredField("lastId");
		lastId.setAccessible(true);
		assertEquals("Incorrect value for lastId", id, lastId.getLong(root));
	}
	
	protected void assertLocation(String expected, String actual) {
		assertTrue("Wrong location: " + actual, actual.indexOf(expected) != -1);
	}
	
	protected void assertLocation(String expected, Subsystem subsystem) {
		assertLocation(expected, subsystem.getLocation());
	}
	
	protected void assertNotChild(Subsystem parent, Subsystem child) {
		assertFalse("Parent contained child", parent.getChildren().contains(child));
	}
	
	protected void assertParent(Subsystem expected, Subsystem subsystem) {
		for (Subsystem parent : subsystem.getParents()) {
			if (parent.equals(expected))
				return;
			
		}
		fail("Parent did not exist: " + expected.getSymbolicName());
	}
	
	protected void assertRegionContextBundle(Subsystem s) {
		Bundle b = getRegionContextBundle(s);
		assertEquals("Not active", Bundle.ACTIVE, b.getState());
		assertEquals("Wrong location", s.getLocation() + '/' + s.getSubsystemId(), b.getLocation());
		assertEquals("Wrong symbolic name", "org.osgi.service.subsystem.region.context." + s.getSubsystemId(), b.getSymbolicName());
		assertEquals("Wrong version", Version.parseVersion("1.0.0"), b.getVersion());
		assertConstituent(s, "org.osgi.service.subsystem.region.context." + s.getSubsystemId(), Version.parseVersion("1.0.0"), ResourceConstants.IDENTITY_TYPE_BUNDLE);
	}
	
	protected void assertState(State expected, State actual) {
		assertState(EnumSet.of(expected), actual);
	}
	
	protected void assertState(EnumSet<State> expected, State actual) {
		assertTrue("Wrong state: " + actual, expected.contains(actual));
	}
	
	protected void assertState(State expected, Subsystem subsystem) {
		assertState(expected, subsystem.getState());
	}
	
	protected void assertState(EnumSet<State> expected, Subsystem subsystem) {
		assertState(expected, subsystem.getState());
	}
	
	protected Subsystem assertSubsystemLifeCycle(File file) throws Exception {
		Subsystem rootSubsystem = getOsgiService(Subsystem.class);
        assertNotNull("Root subsystem was null", rootSubsystem);
        Subsystem subsystem = rootSubsystem.install(file.toURI().toURL().toExternalForm());
        assertNotNull("The subsystem was null", subsystem);
        assertState(EnumSet.of(State.INSTALLING, State.INSTALLED), subsystem.getState());
		assertEvent(subsystem, Subsystem.State.INSTALLING, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		assertEvent(subsystem, Subsystem.State.INSTALLED, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		assertChild(rootSubsystem, subsystem);
        subsystem.start();
        assertState(EnumSet.of(State.RESOLVING, State.RESOLVED, State.STARTING, State.ACTIVE), subsystem.getState());
		assertEvent(subsystem, Subsystem.State.RESOLVING, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		assertEvent(subsystem, Subsystem.State.RESOLVED, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		assertEvent(subsystem, Subsystem.State.STARTING, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		assertEvent(subsystem, Subsystem.State.ACTIVE, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		subsystem.stop();
		assertState(EnumSet.of(State.STOPPING, State.RESOLVED), subsystem.getState());
		assertEvent(subsystem, Subsystem.State.STOPPING, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		assertEvent(subsystem, Subsystem.State.RESOLVED, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		subsystem.uninstall();
		assertState(EnumSet.of(State.UNINSTALLING, State.UNINSTALLED), subsystem.getState());
		assertEvent(subsystem, Subsystem.State.UNINSTALLING, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		assertEvent(subsystem, Subsystem.State.UNINSTALLED, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		assertNotChild(rootSubsystem, subsystem);
		return subsystem;
	}
	
	protected void assertSubsystemNotNull(Subsystem subsystem) {
		assertNotNull("Subsystem was null", subsystem);
	}
	
	protected void assertSymbolicName(String expected, Subsystem subsystem) {
		assertEquals("Wrong symbolic name: " + subsystem.getSymbolicName(), expected, subsystem.getSymbolicName());
	}
	
	protected void assertType(String expected, Subsystem subsystem) {
		assertEquals("Wrong type", expected, subsystem.getType());
	}
	
	protected void assertVersion(String expected, Subsystem subsystem) {
		assertVersion(Version.parseVersion(expected), subsystem);
	}
	
	protected void assertVersion(Version expected, Subsystem subsystem) {
		assertVersion(expected, subsystem.getVersion());
	}
	
	protected void assertVersion(Version expected, Version actual) {
		assertEquals("Wrong version", expected, actual);
	}
	
	protected Bundle getRegionContextBundle(Subsystem subsystem) {
		BundleContext bc = subsystem.getBundleContext();
		assertNotNull("No region context bundle", bc);
		return bc.getBundle();
	}
	
	protected Subsystem getRootSubsystem() {
		return getOsgiService(Subsystem.class);
	}
	
	protected Bundle getSubsystemCoreBundle() {
		return findBundleBySymbolicName("org.apache.aries.subsystem.core");
	}
	
	protected Subsystem installSubsystemFromFile(Subsystem parent, String fileName) throws Exception {
		return installSubsystemFromFile(parent, new File(fileName));
	}
	
	protected Subsystem installSubsystemFromFile(String fileName) throws Exception {
		return installSubsystemFromFile(new File(fileName));
	}
	
	protected Subsystem installSubsystemFromFile(Subsystem parent, File file) throws Exception {
		return installSubsystem(parent, file.toURI().toURL().toExternalForm());
	}
	
	protected Subsystem installSubsystemFromFile(File file) throws Exception {
		return installSubsystem(getRootSubsystem(), file.toURI().toURL().toExternalForm());
	}
	
	protected Subsystem installSubsystem(String location) throws Exception {
		return installSubsystem(getRootSubsystem(), location);
	}
	
	protected Subsystem installSubsystem(String location, InputStream content) throws Exception {
		return installSubsystem(getRootSubsystem(), location, content);
	}
	
	protected Subsystem installSubsystem(Subsystem parent, String location) throws Exception {
		return installSubsystem(parent, location, new URL(location).openStream());
	}
	
	protected Subsystem installSubsystem(Subsystem parent, String location, InputStream content) throws Exception {
		subsystemEvents.clear();
		Subsystem subsystem = getRootSubsystem().install(location, content);
		assertSubsystemNotNull(subsystem);
		assertEvent(subsystem, State.INSTALLING, 5000);
		assertEvent(subsystem, State.INSTALLED, 5000);
		assertChild(parent, subsystem);
		assertLocation(location, subsystem);
		assertParent(parent, subsystem);
		assertState(State.INSTALLED, subsystem);
		assertLocation(location, subsystem);
		assertId(subsystem);
		assertDirectory(subsystem);
		return subsystem;
	}
	
	protected void startSubsystem(Subsystem subsystem) throws Exception {
		assertState(State.INSTALLED, subsystem);
		subsystemEvents.clear();
		subsystem.start();
		assertEvent(subsystem, State.RESOLVING, 5000);
		assertEvent(subsystem, State.RESOLVED, 5000);
		assertEvent(subsystem, State.STARTING, 5000);
		assertEvent(subsystem, State.ACTIVE, 5000);
		assertState(State.ACTIVE, subsystem);
	}
	
	protected void stopSubsystem(Subsystem subsystem) throws Exception {
		assertState(State.ACTIVE, subsystem);
		subsystemEvents.clear();
		subsystem.stop();
		assertEvent(subsystem, State.STOPPING, 5000);
		assertEvent(subsystem, State.RESOLVED, 5000);
		assertState(State.RESOLVED, subsystem);
	}
	
	protected void uninstallSubsystem(Subsystem subsystem) throws Exception {
		assertState(EnumSet.of(State.INSTALLED, State.RESOLVED), subsystem);
		subsystemEvents.clear();
		Collection<Subsystem> parents = subsystem.getParents();
		Bundle b = getRegionContextBundle(subsystem);
		subsystem.uninstall();
		assertEvent(subsystem, State.UNINSTALLING, 5000);
		assertEvent(subsystem, State.UNINSTALLED, 5000);
		assertState(State.UNINSTALLED, subsystem);
		assertEquals("Region context bundle not uninstalled", Bundle.UNINSTALLED, b.getState());
		for (Subsystem parent : parents)
			assertNotChild(parent, subsystem);
		assertNotDirectory(subsystem);
	}
}
