package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.Version;
import org.osgi.service.subsystem.Subsystem;

@RunWith(JUnit4TestRunner.class)
public class RootSubsystemTest extends SubsystemTest {
	// TODO Test root subsystem headers.
	
	@Test
	public void testId() {
		assertEquals("Wrong root ID", getRootSubsystem().getSubsystemId(), 0);
	}
	
	@Test
	public void testLocation() {
		assertEquals("Wrong root location", getRootSubsystem().getLocation(), "subsystem://?Subsystem-SymbolicName=org.osgi.service.subsystem.root&Subsystem-Version=1.0.0");
	}
	
	@Test
	public void testRegionContextBundle() throws BundleException {
		assertRegionContextBundle(getRootSubsystem());
		getSubsystemCoreBundle().stop();
		getSubsystemCoreBundle().start();
		assertRegionContextBundle(getRootSubsystem());
	}
	
	@Test
	public void testServiceEventsFresh() throws Exception {
		Subsystem root = getRootSubsystem();
		Bundle core = getSubsystemCoreBundle();
		core.stop();
		assertEvent(root, Subsystem.State.STOPPING, subsystemEvents.poll(root.getSubsystemId(), 5000));
		assertEvent(root, Subsystem.State.RESOLVED, subsystemEvents.poll(root.getSubsystemId(), 5000));
		// Don't forget about the unregistering event, which will have the same state as before.
		assertEvent(root, Subsystem.State.RESOLVED, subsystemEvents.poll(root.getSubsystemId(), 5000), ServiceEvent.UNREGISTERING);
		core.uninstall();
		core = installBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.core");
		core.start();
		// When starting for the very first time, the root subsystem should transition through all states.
		assertEvent(root, Subsystem.State.INSTALLING, subsystemEvents.poll(root.getSubsystemId(), 5000));
		assertEvent(root, Subsystem.State.INSTALLED, subsystemEvents.poll(root.getSubsystemId(), 5000));
		assertEvent(root, Subsystem.State.RESOLVING, subsystemEvents.poll(root.getSubsystemId(), 5000));
		assertEvent(root, Subsystem.State.RESOLVED, subsystemEvents.poll(root.getSubsystemId(), 5000));
		assertEvent(root, Subsystem.State.STARTING, subsystemEvents.poll(root.getSubsystemId(), 5000));
		assertEvent(root, Subsystem.State.ACTIVE, subsystemEvents.poll(root.getSubsystemId(), 5000));
	}
	
	@Test
	public void testServiceEventsPersisted() throws Exception {
		Subsystem root = getRootSubsystem();
		Bundle core = getSubsystemCoreBundle();
		core.stop();
		assertEvent(root, Subsystem.State.STOPPING, subsystemEvents.poll(root.getSubsystemId(), 5000));
		assertEvent(root, Subsystem.State.RESOLVED, subsystemEvents.poll(root.getSubsystemId(), 5000));
		// Don't forget about the unregistering event, which will have the same state as before.
		assertEvent(root, Subsystem.State.RESOLVED, subsystemEvents.poll(root.getSubsystemId(), 5000), ServiceEvent.UNREGISTERING);
		core.start();
		// On subsequent, post-installation starts, the root subsystem should start in the resolved state.
		assertEvent(root, Subsystem.State.RESOLVED, subsystemEvents.poll(root.getSubsystemId(), 5000), ServiceEvent.REGISTERED);
		assertEvent(root, Subsystem.State.STARTING, subsystemEvents.poll(root.getSubsystemId(), 5000));
		assertEvent(root, Subsystem.State.ACTIVE, subsystemEvents.poll(root.getSubsystemId(), 5000));
	}
	
	@Test
	public void testSymbolicName() {
		assertEquals("Wrong root symbolic name", getRootSubsystem().getSymbolicName(), "org.osgi.service.subsystem.root");
	}
	
	@Test
	public void testVersion() {
		assertEquals("Wrong root version", getRootSubsystem().getVersion(), Version.parseVersion("1.0.0"));
	}
}
