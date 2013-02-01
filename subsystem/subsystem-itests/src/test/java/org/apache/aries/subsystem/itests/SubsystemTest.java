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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.subsystem.AriesSubsystem;
import org.apache.aries.subsystem.core.archive.ProvisionPolicyDirective;
import org.apache.aries.subsystem.core.archive.SubsystemTypeHeader;
import org.apache.aries.subsystem.core.internal.BundleResource;
import org.apache.aries.subsystem.core.internal.ResourceHelper;
import org.apache.aries.subsystem.core.internal.SubsystemIdentifier;
import org.apache.aries.subsystem.itests.util.TestRepository;
import org.apache.aries.subsystem.itests.util.Utils;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.JarFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ManifestFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.apache.aries.util.filesystem.FileSystem;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;
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
				// Felix mvn url handler
				mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),
				// Bundles
				mavenBundle("org.apache.aries",             "org.apache.aries.util").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.api").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils").versionAsInProject(),
				mavenBundle("org.apache.aries.blueprint",   "org.apache.aries.blueprint").versionAsInProject(),
				mavenBundle("org.apache.aries.proxy",       "org.apache.aries.proxy").versionAsInProject(),
				mavenBundle("org.apache.aries.subsystem",   "org.apache.aries.subsystem.api").versionAsInProject(),
				// The itests need private packages from the core subsystems bundle.
				CoreOptions.provision(SubsystemTest.class.getClassLoader().getResourceAsStream("core.fragment/core.fragment.jar")),
				mavenBundle("org.apache.aries.subsystem",   "org.apache.aries.subsystem.core").versionAsInProject(),
				mavenBundle("org.apache.aries.subsystem",   "org.apache.aries.subsystem.itest.interfaces").versionAsInProject(),
				mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit").versionAsInProject(),
				mavenBundle("org.apache.felix",             "org.apache.felix.resolver"),
				mavenBundle("org.eclipse.equinox",          "org.eclipse.equinox.coordinator").version("1.1.0.v20120522-1841"),
				mavenBundle("org.eclipse.equinox",          "org.eclipse.equinox.event").version("1.2.200.v20120522-2049"),
				mavenBundle("org.eclipse.equinox",          "org.eclipse.equinox.region").version("1.1.0.v20120522-1841"),
				mavenBundle("org.osgi",                     "org.osgi.enterprise").version("5.0.0"),
				mavenBundle("org.easymock",					"easymock").versionAsInProject(),
//				org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7777"),
				PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),
				equinox().version("3.8.0.V20120529-1548"));
		options = updateOptions(options);
		return options;
	}
	
	protected final SubsystemEventHandler subsystemEvents = new SubsystemEventHandler();
	
	protected Collection<ServiceRegistration<?>> serviceRegistrations = new ArrayList<ServiceRegistration<?>>();
	
	public void setUp() throws Exception {
		super.setUp();
		try {
			bundleContext.getBundle(0).getBundleContext().addServiceListener(subsystemEvents, '(' + Constants.OBJECTCLASS + '=' + Subsystem.class.getName() + ')');
		}
		catch (InvalidSyntaxException e) {
			fail("Invalid filter: " + e.getMessage());
		}
		assertSubsystemNotNull(getRootSubsystem());
	}
	
	public void tearDown() throws Exception 
	{
		bundleContext.removeServiceListener(subsystemEvents);
		for (ServiceRegistration<?> registration : serviceRegistrations)
			Utils.unregisterQuietly(registration);
		serviceRegistrations.clear();
		super.tearDown();
	}
	
	protected void assertEmptySubsystem(Subsystem subsystem) {
    	assertSymbolicName("org.apache.aries.subsystem.itests.subsystem.empty", subsystem);
    	assertVersion("0", subsystem);
    	assertType(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, subsystem);
    }
	
	protected void assertBundleState(int state, String symbolicName, Subsystem subsystem) {
    	Bundle bundle = getBundle(subsystem, symbolicName);
    	assertBundleState(bundle, state);
    }
	
	protected void assertBundleState(Bundle bundle, int state) {
		assertNotNull("Bundle not found: " + bundle, bundle);
		assertTrue("Wrong state: " + bundle + " [expected " + state + " but was " + bundle.getState() + "]", (bundle.getState() & state) != 0);
	}
	
	protected Subsystem assertChild(Subsystem parent, String symbolicName) {
		return assertChild(parent, symbolicName, null, null);
	}
	
	protected Subsystem assertChild(Subsystem parent, String symbolicName, Version version) {
		return assertChild(parent, symbolicName, version, null);
	}
	
	protected Subsystem assertChild(Subsystem parent, String symbolicName, Version version, String type) {
		Subsystem result = getChild(parent, symbolicName, version, type);
		assertNotNull("Child does not exist: " + symbolicName, result);
		return result;
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
	
	protected void assertClassLoadable(String clazz, Bundle bundle) {
		try {
			bundle.loadClass(clazz);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Class " + clazz + " from bundle " + bundle + " should be loadable");
		}
	}
	
	protected void assertConstituent(Subsystem subsystem, String symbolicName) {
		assertConstituent(subsystem, symbolicName, Version.emptyVersion);
	}
	
	protected void assertConstituent(Subsystem subsystem, String symbolicName, Version version) {
		assertConstituent(subsystem, symbolicName, version, IdentityNamespace.TYPE_BUNDLE);
	}
	
	protected void assertContituent(Subsystem subsystem, String symbolicName, String type) {
		assertConstituent(subsystem, symbolicName, Version.emptyVersion, type);
	}
	
	protected Resource assertConstituent(Subsystem subsystem, String symbolicName, Version version, String type) {
		Resource constituent = getConstituent(subsystem, symbolicName, version, type);
		assertNotNull("Constituent not found: " + symbolicName + ';' + version + ';' + type, constituent);
		return constituent;
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
		assertNotNull("No event", event);
		assertEquals("Wrong ID", subsystem.getSubsystemId(), event.getId());
		assertEquals("Wrong symbolic name", subsystem.getSymbolicName(), event.getSymbolicName());
		assertEquals("Wrong version", subsystem.getVersion(), event.getVersion());
		assertEquals("Wrong type", subsystem.getType(), event.getType());
		assertEquals("Wrong state", state, event.getState());
		assertEquals("Wrong event type", type, event.getEventType());
	}
	
	protected String assertHeaderExists(Subsystem subsystem, String name) {
		String header = subsystem.getSubsystemHeaders(null).get(name);
		assertNotNull("Missing header: " + name, header);
		return header;
	}
	
	protected void assertId(Subsystem subsystem) {
		assertId(subsystem.getSubsystemId());
	}
	
	protected void assertId(Long id) {
		assertTrue("Subsystem ID was not a positive integer: " + id, id > 0);
	}
	
	protected void assertLastId(long id) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Subsystem root = getRootSubsystem();
		Field lastId = SubsystemIdentifier.class.getDeclaredField("lastId");
		lastId.setAccessible(true);
		assertEquals("Wrong lastId", id, lastId.getLong(root));
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
	
	protected void assertNotConstituent(Subsystem subsystem, String symbolicName) {
		assertNotConstituent(subsystem, symbolicName, Version.emptyVersion, IdentityNamespace.TYPE_BUNDLE);
	}
	
	protected void assertNotConstituent(Subsystem subsystem, String symbolicName, Version version, String type) {
		Resource constituent = getConstituent(subsystem, symbolicName, version, type);
		assertNull("Constituent found: " + symbolicName + ';' + version + ';' + type, constituent);
	}
	
	protected void assertParent(Subsystem expected, Subsystem subsystem) {
		for (Subsystem parent : subsystem.getParents()) {
			if (parent.equals(expected))
				return;
			
		}
		fail("Parent did not exist: " + expected.getSymbolicName());
	}
	
	protected void assertProvisionPolicy(Subsystem subsystem, boolean acceptsDependencies) {
		String headerStr = subsystem.getSubsystemHeaders(null).get(SubsystemConstants.SUBSYSTEM_TYPE);
		assertNotNull("Missing subsystem type header", headerStr);
		SubsystemTypeHeader header = new SubsystemTypeHeader(headerStr);
		ProvisionPolicyDirective directive = header.getProvisionPolicyDirective();
		if (acceptsDependencies)
			assertTrue("Subsystem does not accept dependencies", directive.isAcceptDependencies());
		else
			assertTrue("Subsystem accepts dependencies", directive.isRejectDependencies());
	}
	
	protected void assertRefresh(Collection<Bundle> bundles) throws InterruptedException {
		FrameworkWiring wiring = getSystemBundleAsFrameworkWiring();
		final AtomicBoolean refreshed = new AtomicBoolean(false);
		wiring.refreshBundles(bundles, new FrameworkListener() {
			@Override
			public void frameworkEvent(FrameworkEvent event) {
				if (FrameworkEvent.PACKAGES_REFRESHED == event.getType()) {
					synchronized (refreshed) {
						refreshed.set(true);
						refreshed.notify();
					}
				}
			}
		});
		synchronized (refreshed) {
			refreshed.wait(5000);
		}
		assertTrue("Bundles not refreshed", refreshed.get());
	}
	
	protected void assertRefreshAndResolve(Collection<Bundle> bundles) throws InterruptedException {
		assertRefresh(bundles);
		assertResolve(bundles);
	}
	
	protected void assertRegionContextBundle(Subsystem s) {
		Bundle b = getRegionContextBundle(s);
		assertEquals("Not active", Bundle.ACTIVE, b.getState());
		assertEquals("Wrong location", s.getLocation() + '/' + s.getSubsystemId(), b.getLocation());
		assertEquals("Wrong symbolic name", "org.osgi.service.subsystem.region.context." + s.getSubsystemId(), b.getSymbolicName());
		assertEquals("Wrong version", Version.parseVersion("1.0.0"), b.getVersion());
		assertConstituent(s, "org.osgi.service.subsystem.region.context." + s.getSubsystemId(), Version.parseVersion("1.0.0"), IdentityNamespace.TYPE_BUNDLE);
	}
	
	protected void assertResolve(Collection<Bundle> bundles) {
		FrameworkWiring wiring = getSystemBundleAsFrameworkWiring();
		assertTrue("Bundles not resolved", wiring.resolveBundles(bundles));
	}
	
	protected void assertServiceEventsInstall(Subsystem subsystem) throws InterruptedException {
		assertEvent(subsystem, Subsystem.State.INSTALLING, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		assertEvent(subsystem, Subsystem.State.INSTALLED, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
	}
	
	protected void assertServiceEventsResolve(Subsystem subsystem) throws InterruptedException {
		assertEvent(subsystem, Subsystem.State.RESOLVING, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		assertServiceEventResolved(subsystem, ServiceEvent.MODIFIED);
	}
	
	protected void assertServiceEventsStart(Subsystem subsystem) throws InterruptedException {
		assertEvent(subsystem, Subsystem.State.STARTING, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		assertEvent(subsystem, Subsystem.State.ACTIVE, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
	}
	
	protected void assertServiceEventsStop(Subsystem subsystem) throws InterruptedException {
		assertEvent(subsystem, Subsystem.State.STOPPING, subsystemEvents.poll(subsystem.getSubsystemId(), 5000));
		assertServiceEventResolved(subsystem, ServiceEvent.MODIFIED);
		// Don't forget about the unregistering event, which will have the same state as before.
		assertServiceEventResolved(subsystem, ServiceEvent.UNREGISTERING);
	}
	
	protected void assertServiceEventResolved(Subsystem subsystem, int type) throws InterruptedException {
		assertEvent(subsystem, Subsystem.State.RESOLVED, subsystemEvents.poll(subsystem.getSubsystemId(), 5000), type);
	}
	
	protected void assertStartLevel(Bundle bundle, int expected) {
		assertEquals("Wrong start level", expected, bundle.adapt(BundleStartLevel.class).getStartLevel());
	}
	
	protected void assertState(State expected, State actual) {
		assertState(EnumSet.of(expected), actual);
	}
	
	protected void assertState(EnumSet<State> expected, State actual) {
		assertTrue("Wrong state: expected=" + expected + ", actual=" + actual, expected.contains(actual));
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
		assertSymbolicName(expected, subsystem.getSymbolicName());
	}
	
	protected void assertSymbolicName(String expected, String actual) {
		assertEquals("Wrong symbolic name", expected, actual);
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
	
	protected static void createBundle(String symbolicName) throws IOException {
		createBundle(symbolicName, null);
	}
	
	protected static void createBundle(String symbolicName, Map<String, String> headers) throws IOException {
		createBundle(symbolicName, null, headers);
	}
	
	protected static void createBundle(String symbolicName, String version, Map<String, String> headers) throws IOException {
		if (headers == null)
			headers = new HashMap<String, String>();
		headers.put(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
		if (version != null)
			headers.put(Constants.BUNDLE_VERSION, version);
		createBundle(headers);
	}
	
	protected static void createBundle(Map<String, String> headers) throws IOException 
	{
		String symbolicName = headers.get(Constants.BUNDLE_SYMBOLICNAME);
		JarFixture bundle = ArchiveFixture.newJar();
		ManifestFixture manifest = bundle.manifest();
		for (Entry<String, String> header : headers.entrySet()) {
			manifest.attribute(header.getKey(), header.getValue());
		}
		write(symbolicName, bundle);
	}
	
	protected RepositoryContent createBundleRepositoryContent(String file) throws Exception {
		return createBundleRepositoryContent(new File(file));
	}
	
	protected RepositoryContent createBundleRepositoryContent(File file) throws Exception {
		return new BundleResource(FileSystem.getFSRoot(file));
	}
	
	protected static void createManifest(String name, Map<String, String> headers) throws IOException {
		ManifestFixture manifest = ArchiveFixture.newJar().manifest();
		for (Entry<String, String> header : headers.entrySet()) {
			manifest.attribute(header.getKey(), header.getValue());
		}
		write(name, manifest);
	}
	
	protected static void createSubsystem(String name) throws IOException {
		createSubsystem(name, new String[0]);
	}
	
	protected static void createSubsystem(String name, String...contents) throws IOException {
		File manifest = new File(name + ".mf");
		ZipFixture fixture = ArchiveFixture.newZip();
		if (manifest.exists())
			// The following input stream is closed by ArchiveFixture.copy.
			fixture.binary("OSGI-INF/SUBSYSTEM.MF", new FileInputStream(name + ".mf"));
		if (contents != null) {
			for (String content : contents) {
				// The following input stream is closed by ArchiveFixture.copy.
				fixture.binary(content, new FileInputStream(content));
			}
		}
		write(name, fixture);
	}
	
	protected Subsystem findSubsystemService(long id) throws InvalidSyntaxException {
		String filter = "(" + SubsystemConstants.SUBSYSTEM_ID_PROPERTY + "=" + id + ")";
		return getOsgiService(Subsystem.class, filter, 5000);
	}
	
	protected Bundle getBundle(Subsystem subsystem, String symbolicName) {
		for (Bundle bundle : subsystem.getBundleContext().getBundles()) {
			if (symbolicName.equals(bundle.getSymbolicName())) { 
				return bundle;
			}
		}
		return null;
	}
	
	protected Subsystem getChild(Subsystem parent, String symbolicName) {
		return getChild(parent, symbolicName, null, null);
	}
	
	protected Subsystem getChild(Subsystem parent, String symbolicName, Version version) {
		return getChild(parent, symbolicName, version, null);
	}
	
	protected Subsystem getChild(Subsystem parent, String symbolicName, Version version, String type) {
		for (Subsystem child : parent.getChildren()) {
			if (symbolicName.equals(child.getSymbolicName())) {
				if (version == null)
					version = Version.emptyVersion;
				if (version.equals(child.getVersion())) {
					if (type == null)
						type = SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION;
					if (type.equals(child.getType())) {
						return child;
					}
				}
			}
		}
		return null;
	}
	
	protected Resource getConstituent(Subsystem subsystem, String symbolicName, Version version, String type) {
		for (Resource resource : subsystem.getConstituents()) {
			if (symbolicName.equals(ResourceHelper.getSymbolicNameAttribute(resource))) {
				if (version == null)
					version = Version.emptyVersion;
				if (version.equals(ResourceHelper.getVersionAttribute(resource))) {
					if (type == null)
						type = IdentityNamespace.TYPE_BUNDLE;
					if (type.equals(ResourceHelper.getTypeAttribute(resource))) {
						return resource;
					}
				}
			}
		}
		return null;
	}
	
	protected AriesSubsystem getConstituentAsAriesSubsystem(Subsystem subsystem, String symbolicName, Version version, String type) {
		Resource resource = getConstituent(subsystem, symbolicName, version, type);
		return (AriesSubsystem)resource;
	}
	
	protected Bundle getConstituentAsBundle(Subsystem subsystem, String symbolicName, Version version, String type) {
		return getConstituentAsBundleRevision(subsystem, symbolicName, version, type).getBundle();
	}
	
	protected BundleRevision getConstituentAsBundleRevision(Subsystem subsystem, String symbolicName, Version version, String type) {
		Resource resource = getConstituent(subsystem, symbolicName, version, type);
		return (BundleRevision)resource;
	}
	
	protected Subsystem getConstituentAsSubsystem(Subsystem subsystem, String symbolicName, Version version, String type) {
		Resource resource = getConstituent(subsystem, symbolicName, version, type);
		return (Subsystem)resource;
	}
	
	protected Region getRegion(Subsystem subsystem) {
		RegionDigraph digraph = getOsgiService(RegionDigraph.class);
		String name = subsystem.getSymbolicName() + ';' + subsystem.getVersion() + ';' + subsystem.getType() + ';' + subsystem.getSubsystemId();
		Region region = digraph.getRegion(name);
		assertNotNull("Region not found: " + name, region);
		return region;
	}
	
	protected Bundle getRegionContextBundle(Subsystem subsystem) {
		BundleContext bc = subsystem.getBundleContext();
		assertNotNull("No region context bundle", bc);
		return bc.getBundle();
	}
	
	protected AriesSubsystem getRootAriesSubsystem() {
		return getOsgiService(AriesSubsystem.class);
	}
	
	protected Subsystem getRootSubsystem() {
		return getOsgiService(Subsystem.class);
	}
	
	protected Bundle getSystemBundle() {
		return bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
	}
	
	protected FrameworkStartLevel getSystemBundleAsFrameworkStartLevel() {
		return getSystemBundle().adapt(FrameworkStartLevel.class);
	}
	
	protected FrameworkWiring getSystemBundleAsFrameworkWiring() {
		return getSystemBundle().adapt(FrameworkWiring.class);
	}
	
	protected Bundle getSubsystemCoreBundle() {
		return findBundleBySymbolicName("org.apache.aries.subsystem.core");
	}
	
	protected Bundle installBundleFromFile(String fileName) throws FileNotFoundException, BundleException {
		return installBundleFromFile(new File(fileName));
	}
	
	protected Bundle installBundleFromFile(File file) throws FileNotFoundException, BundleException {
		return installBundleFromFile(file, getRootSubsystem());
	}
	
	protected Bundle installBundleFromFile(String file, Subsystem subsystem) throws FileNotFoundException, BundleException {
		return installBundleFromFile(new File(file), subsystem);
	}
	
	protected Bundle installBundleFromFile(File file, Subsystem subsystem) throws FileNotFoundException, BundleException {
		Bundle bundle = installBundleFromFile(file, subsystem.getBundleContext());
		assertBundleState(Bundle.INSTALLED|Bundle.RESOLVED, bundle.getSymbolicName(), subsystem);
		return bundle;
	}
	
	protected Bundle installBundleFromFile(File file, BundleContext bundleContext) throws FileNotFoundException, BundleException {
		// The following input stream is closed by the bundle context.
		return bundleContext.installBundle(file.toURI().toString(), new FileInputStream(file));
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
		// The following input stream is closed by Subsystem.install.
		return installSubsystem(parent, location, new URL(location).openStream());
	}
	
	protected Subsystem installSubsystem(Subsystem parent, String location, InputStream content) throws Exception {
		subsystemEvents.clear();
		Subsystem subsystem = parent.install(location, content);
		assertSubsystemNotNull(subsystem);
		assertEvent(subsystem, State.INSTALLING, 5000);
		assertEvent(subsystem, State.INSTALLED, 5000);
		assertChild(parent, subsystem);
		assertLocation(location, subsystem);
		assertParent(parent, subsystem);
		assertState(State.INSTALLED, subsystem);
		assertLocation(location, subsystem);
		assertId(subsystem);
		// TODO This does not take into account nested directories.
//		assertDirectory(subsystem);
		return subsystem;
	}
	
	protected void registerRepositoryService(Repository repository) {
		serviceRegistrations.add(bundleContext.registerService(
				Repository.class, repository, null));
	}
	
	protected void registerRepositoryService(Resource...resources) {
		TestRepository.Builder builder = new TestRepository.Builder();
		for (Resource resource : resources) {
			builder.resource(resource);
		}
		registerRepositoryService(builder.build());
	}
	
	protected void registerRepositoryService(String...files) throws Exception {
		Resource[] resources = new Resource[files.length];
		int i = 0;
		for (String file : files) {
			resources[i++] = (Resource)createBundleRepositoryContent(file);
		}
		registerRepositoryService(resources);
	}
	
	protected void restartSubsystemsImplBundle() throws BundleException {
		Bundle b = getSubsystemCoreBundle();
		b.stop();
		b.start();
	}
	
	protected void startBundle(Bundle bundle) throws BundleException {
		startBundle(bundle, getRootSubsystem());
	}
	
	protected void startBundle(Bundle bundle, Subsystem subsystem) throws BundleException {
		bundle.start();
		assertBundleState(Bundle.ACTIVE, bundle.getSymbolicName(), subsystem);
	}
	
	protected void startSubsystem(Subsystem subsystem) throws Exception {
		startSubsystemFromInstalled(subsystem);
	}
	
	protected void startSubsystemFromInstalled(Subsystem subsystem) throws InterruptedException {
		assertState(State.INSTALLED, subsystem);
		subsystemEvents.clear();
		subsystem.start();
		assertEvent(subsystem, State.RESOLVING, 5000);
		assertEvent(subsystem, State.RESOLVED, 5000);
		assertEvent(subsystem, State.STARTING, 5000);
		assertEvent(subsystem, State.ACTIVE, 5000);
		assertState(State.ACTIVE, subsystem);
	}
	
	protected void startSubsystemFromResolved(Subsystem subsystem) throws InterruptedException {
		assertState(State.RESOLVED, subsystem);
		subsystemEvents.clear();
		subsystem.start();
		assertEvent(subsystem, State.STARTING, 5000);
		assertEvent(subsystem, State.ACTIVE, 5000);
		assertState(State.ACTIVE, subsystem);
	}
	
	protected void stopAndUninstallSubsystemSilently(Subsystem subsystem) {
		stopSubsystemSilently(subsystem);
		uninstallSubsystemSilently(subsystem);
	}
	
	protected void stopSubsystem(Subsystem subsystem) throws Exception {
		assertState(State.ACTIVE, subsystem);
		subsystemEvents.clear();
		subsystem.stop();
		assertEvent(subsystem, State.STOPPING, 5000);
		assertEvent(subsystem, State.RESOLVED, 5000);
		assertState(State.RESOLVED, subsystem);
	}
	
	protected void stopSubsystemSilently(Subsystem subsystem) {
		try {
			stopSubsystem(subsystem);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	protected void uninstallSilently(Bundle bundle) {
		if (bundle == null)
			return;
		try {
			bundle.uninstall();
		}
		catch (Exception e) {}
	}
	
	protected void uninstallSubsystem(Subsystem subsystem) throws Exception {
		assertState(EnumSet.of(State.INSTALLED, State.RESOLVED), subsystem);
		subsystemEvents.clear();
		Collection<Subsystem> parents = subsystem.getParents();
		Bundle b = null;
		Region region = null;
		RegionDigraph digraph = getOsgiService(RegionDigraph.class);
		if (subsystem.getType().equals(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION)
				|| subsystem.getType().equals(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE)) {
			b = getRegionContextBundle(subsystem);
			region = digraph.getRegion(b);
		}
		State state = subsystem.getState();
		subsystem.uninstall();
		if (!EnumSet.of(State.INSTALL_FAILED, State.INSTALLED, State.INSTALLING).contains(state))
			assertEvent(subsystem, State.INSTALLED, 5000);
		assertEvent(subsystem, State.UNINSTALLING, 5000);
		assertEvent(subsystem, State.UNINSTALLED, 5000);
		assertState(State.UNINSTALLED, subsystem);
		for (Subsystem parent : parents)
			assertNotChild(parent, subsystem);
//		assertNotDirectory(subsystem);
		if (subsystem.getType().equals(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION)
				|| subsystem.getType().equals(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE)) {
			assertEquals("Region context bundle not uninstalled", Bundle.UNINSTALLED, b.getState());
			assertNull("Region not removed", digraph.getRegion(region.getName()));
		}
	}
	
	protected void uninstallSubsystemSilently(Subsystem subsystem) {
		if (subsystem == null)
			return;
		try {
			uninstallSubsystem(subsystem);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	protected static void write(String file, ArchiveFixture.AbstractFixture fixture) throws IOException 
	{
		write(new File(file), fixture);
	}
	
	protected static void write(File file, ArchiveFixture.AbstractFixture fixture) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
    	try {
    		fixture.writeOut(fos);
    	}
    	finally {
    		fos.close();
    	}
	}
	
	static void createApplication(String name, String[] content) throws Exception 
	{
		ZipFixture feature = ArchiveFixture
				.newZip()
				.binary("OSGI-INF/SUBSYSTEM.MF",
						// The following input stream is closed by ArchiveFixture.copy.
						SubsystemTest.class.getClassLoader().getResourceAsStream(
								name + "/OSGI-INF/SUBSYSTEM.MF"));
		for (String s : content) {
			try {
				feature.binary(s,
						// The following input stream is closed by ArchiveFixture.copy.
						SubsystemTest.class.getClassLoader().getResourceAsStream(
								name + '/' + s));
			}
			catch (Exception e) {
				// The following input stream is closed by ArchiveFixture.copy.
				feature.binary(s, new FileInputStream(new File(s)));
			}
		}
		feature.end();
		FileOutputStream fos = new FileOutputStream(name + ".esa");
		try {
			feature.writeOut(fos);
		} finally {
			Utils.closeQuietly(fos);
		}
	}
	
	protected static String normalizeBundleLocation(String location) {
		if (location.startsWith("initial@"))
			return location.substring(8);
		return location;
	}
}
