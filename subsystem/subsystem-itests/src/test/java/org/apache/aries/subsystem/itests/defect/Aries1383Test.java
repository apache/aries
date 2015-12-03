package org.apache.aries.subsystem.itests.defect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.AriesSubsystem;
import org.apache.aries.subsystem.core.archive.AriesProvisionDependenciesDirective;
import org.apache.aries.subsystem.core.archive.SubsystemTypeHeader;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.BundleArchiveBuilder;
import org.apache.aries.subsystem.itests.util.SubsystemArchiveBuilder;
import org.apache.aries.subsystem.itests.util.TestCapability;
import org.apache.aries.subsystem.itests.util.TestRepository;
import org.apache.aries.subsystem.itests.util.TestRepositoryContent;
import org.apache.aries.subsystem.itests.util.TestRequirement;
import org.easymock.internal.matchers.Null;
import org.junit.Test;
import org.ops4j.pax.tinybundles.core.InnerClassStrategy;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

/*
 * https://issues.apache.org/jira/browse/ARIES-1383
 * 
 * Provide option to disable the provisioning of dependencies at install time.
 * 
 * For tests containing a numerical value in the name, see the description of
 * ARIES-1383 for an explanation.
 */
public class Aries1383Test extends SubsystemTest {
	private static final String SYMBOLICNAME_PREFIX = Aries1383Test.class.getSimpleName() + '.';
	
	private static final String APPLICATION_A = SYMBOLICNAME_PREFIX + "application.a";
	private static final String APPLICATION_B = SYMBOLICNAME_PREFIX + "application.b";
	private static final String APPLICATION_DEPENDENCY_IN_ARCHIVE = SYMBOLICNAME_PREFIX + "application.dependency.in.archive";
	private static final String APPLICATION_EMPTY = SYMBOLICNAME_PREFIX + "application.empty";
	private static final String APPLICATION_INSTALL_FAILED = SYMBOLICNAME_PREFIX + "application.install.failed";
	private static final String APPLICATION_INVALID_PROVISION_DEPENDENCIES = SYMBOLICNAME_PREFIX + "application.invalid.provision.dependency";
	private static final String APPLICATION_MISSING_DEPENDENCY = SYMBOLICNAME_PREFIX + "application.missing.dependency";
	private static final String APPLICATION_PROVISION_DEPENDENCIES_INSTALL = SYMBOLICNAME_PREFIX + "application.provision.dependencies.install";
	private static final String APPLICATION_START_FAILURE = SYMBOLICNAME_PREFIX + "application.start.failure";
	private static final String BUNDLE_A = SYMBOLICNAME_PREFIX + "bundle.a";
	private static final String BUNDLE_B = SYMBOLICNAME_PREFIX + "bundle.b";
	private static final String BUNDLE_C = SYMBOLICNAME_PREFIX + "bundle.c";
	private static final String BUNDLE_D = SYMBOLICNAME_PREFIX + "bundle.d";
	private static final String BUNDLE_INVALID_MANIFEST = SYMBOLICNAME_PREFIX + "bundle.invalid.manifest";
	private static final String BUNDLE_START_FAILURE = SYMBOLICNAME_PREFIX + "bundle.start.failure";
	private static final String ESA_EXTENSION = ".esa";
	private static final String FEATURE_PROVISION_DEPENDENCIES_INSTALL = "feature.provision.dependencies.install";
	private static final String FEATURE_PROVISION_DEPENDENCIES_RESOLVE = "feature.provision.dependencies.resolve";
	private static final String JAR_EXTENSION = ".jar";
	private static final String MANIFEST_VERSION = "1.0";
	private static final String PACKAGE_A = SYMBOLICNAME_PREFIX + "a";
	private static final String PACKAGE_B = SYMBOLICNAME_PREFIX + "b";
	private static final String PACKAGE_C = SYMBOLICNAME_PREFIX + "c";
	private static final String PACKAGE_D = SYMBOLICNAME_PREFIX + "d";
	private static final String SUBSYSTEM_MANIFEST_FILE = "OSGI-INF/SUBSYSTEM.MF";
	
	private final List<Subsystem> stoppableSubsystems = new ArrayList<Subsystem>();
	private final List<Subsystem> uninstallableSubsystems = new ArrayList<Subsystem>();
	
	/*
	 * (1) A set of subsystems with interleaving content dependencies are able 
	 * to be independently, simultaneously, and successfully installed and 
	 * started.
	 */
	@Test
	public void test1() throws Exception {
		Subsystem root = getRootSubsystem();
		final Subsystem c1 = installSubsystem(
				root,
				"c1", 
				new SubsystemArchiveBuilder()
						.symbolicName("c1")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE + ';' 
										+ AriesProvisionDependenciesDirective.RESOLVE.toString())
						.build(),
				false
		);
		uninstallableSubsystems.add(c1);
		c1.start();
		stoppableSubsystems.add(c1);
		@SuppressWarnings("unchecked")
		Callable<Subsystem>[] installCallables = new Callable[] {
				new Callable<Subsystem>() {
					@Override
					public Subsystem call() throws Exception {
						return installSubsystem(
								c1,
								"a1", 
								new SubsystemArchiveBuilder()
										.symbolicName("a1")
										.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' 
														+ AriesProvisionDependenciesDirective.RESOLVE.toString())
										.content("b1")
										.bundle(
												"b1", 
												new BundleArchiveBuilder()
														.symbolicName("b1")
														.importPackage("b2")
														.build())
										.build(),
								false);
					}
					
				},
				new Callable<Subsystem>() {
					@Override
					public Subsystem call() throws Exception {
						return installSubsystem(
								c1,
								"f1", 
								new SubsystemArchiveBuilder()
										.symbolicName("f1")
										.type(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE + ';' 
														+ AriesProvisionDependenciesDirective.RESOLVE.toString())
										.content("b2")
										.bundle(
												"b2", 
												new BundleArchiveBuilder()
														.symbolicName("b2")
														.exportPackage("b2")
														.importPackage("b3")
														.build())
										.build(),
								false);
					}
					
				},
				new Callable<Subsystem>() {
					@Override
					public Subsystem call() throws Exception {
						return installSubsystem(
								c1,
								"f2", 
								new SubsystemArchiveBuilder()
										.symbolicName("f2")
										.type(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE + ';' 
														+ AriesProvisionDependenciesDirective.RESOLVE.toString())
										.content("b4")
										.bundle(
												"b4", 
												new BundleArchiveBuilder()
														.symbolicName("b4")
														.exportPackage("b4")
														.importPackage("b2")
														.importPackage("b3")
														.build())
										.build(),
								false);
					}
					
				},
				new Callable<Subsystem>() {
					@Override
					public Subsystem call() throws Exception {
						return installSubsystem(
								c1,
								"c2", 
								new SubsystemArchiveBuilder()
										.symbolicName("c2")
										.type(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE + ';' 
														+ AriesProvisionDependenciesDirective.RESOLVE.toString())
										.content("b3;version=\"[0,0]\"")
										.exportPackage("b3")
										.importPackage("b4")
										.bundle(
												"b3", 
												new BundleArchiveBuilder()
														.symbolicName("b3")
														.exportPackage("b3")
														.importPackage("b4")
														.build())
										.build(),
								false);
					}
					
				}
		};
		ExecutorService executor = Executors.newFixedThreadPool(4);
		List<Future<Subsystem>> installFutures = executor.invokeAll(Arrays.asList(installCallables));
		final Subsystem a1 = installFutures.get(0).get();
		uninstallableSubsystems.add(a1);
		assertConstituent(a1, "b1");
		final Subsystem f1 = installFutures.get(1).get();
		uninstallableSubsystems.add(f1);
		assertConstituent(f1, "b2");
		final Subsystem f2 = installFutures.get(2).get();
		uninstallableSubsystems.add(f2);
		assertConstituent(f2, "b4");
		final Subsystem c2 = installFutures.get(3).get();
		uninstallableSubsystems.add(c2);
		assertConstituent(c2, "b3");
		@SuppressWarnings("unchecked")
		Callable<Null>[] startCallables = new Callable[] {
			new Callable<Null>() {
				@Override
				public Null call() throws Exception {
					a1.start();
					assertEvent(a1, State.INSTALLED, subsystemEvents.poll(a1.getSubsystemId(), 5000));
					assertEvent(a1, State.RESOLVING, subsystemEvents.poll(a1.getSubsystemId(), 5000));
					assertEvent(a1, State.RESOLVED, subsystemEvents.poll(a1.getSubsystemId(), 5000));
					return null;
				}
			},
			new Callable<Null>() {
				@Override
				public Null call() throws Exception {
					f1.start();
					assertEvent(f1, State.INSTALLED, subsystemEvents.poll(f1.getSubsystemId(), 5000));
					assertEvent(f1, State.RESOLVING, subsystemEvents.poll(f1.getSubsystemId(), 5000));
					assertEvent(f1, State.RESOLVED, subsystemEvents.poll(f1.getSubsystemId(), 5000));
					return null;
				}
			},
			new Callable<Null>() {
				@Override
				public Null call() throws Exception {
					f2.start();
					assertEvent(f2, State.INSTALLED, subsystemEvents.poll(f2.getSubsystemId(), 5000));
					assertEvent(f2, State.RESOLVING, subsystemEvents.poll(f2.getSubsystemId(), 5000));
					assertEvent(f2, State.RESOLVED, subsystemEvents.poll(f2.getSubsystemId(), 5000));
					return null;
				}
			},
			new Callable<Null>() {
				@Override
				public Null call() throws Exception {
					c2.start();
					assertEvent(c2, State.INSTALLED, subsystemEvents.poll(c2.getSubsystemId(), 5000));
					assertEvent(c2, State.RESOLVING, subsystemEvents.poll(c2.getSubsystemId(), 5000));
					assertEvent(c2, State.RESOLVED, subsystemEvents.poll(c2.getSubsystemId(), 5000));
					return null;
				}
			}
		};
		List<Future<Null>> startFutures = executor.invokeAll(Arrays.asList(startCallables));
		startFutures.get(0).get();
		stoppableSubsystems.add(a1);
		startFutures.get(1).get();
		stoppableSubsystems.add(f1);
		startFutures.get(2).get();
		stoppableSubsystems.add(f2);
		startFutures.get(3).get();
		stoppableSubsystems.add(c2);
	}
	
	/*
	 * (2) Subsystem with apache-aries-provision-dependencies:=resolve is in the 
	 * INSTALLING state after a successful installation.
	 */
	@Test
	public void test2() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem subsystem = installSubsystem(root, APPLICATION_EMPTY, applicationEmpty(), false);
		try {
			assertState(State.INSTALLING, subsystem);
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	/*
	 * (3) Subsystem with apache-aries-provision-dependencies:=resolve is available 
	 * as a service after a successful installation.
	 */
	@Test
	public void test3() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem subsystem = installSubsystem(root, APPLICATION_EMPTY, applicationEmpty(), false);
		try {
			assertReferences(
					Subsystem.class, 
					"(&(subsystem.symbolicName=" 
							+ APPLICATION_EMPTY 
							+ ")(subsystem.version=0.0.0)(subsystem.type=" 
							+ SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION 
							+ ")(subsystem.state=" 
							+ State.INSTALLING 
							+ "))",
					1);
			assertReferences(
					AriesSubsystem.class, 
					"(&(subsystem.symbolicName=" 
							+ APPLICATION_EMPTY 
							+ ")(subsystem.version=0.0.0)(subsystem.type=" 
							+ SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION 
							+ ")(subsystem.state=" 
							+ State.INSTALLING 
							+ "))",
					1);
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	/*
	 * (4) Subsystem with apache-aries-provision-dependencies:=resolve does not 
	 * have its dependencies installed after a successful installation.
	 */
	@Test
	public void test4() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem subsystem = installSubsystem(root, APPLICATION_DEPENDENCY_IN_ARCHIVE, applicationDependencyInArchive(), false);
		try {
			assertConstituent(subsystem, BUNDLE_A);
			assertNotConstituent(subsystem, BUNDLE_B);
			assertNotConstituent(root, BUNDLE_B);
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	/*
	 * (5) Subsystem with apache-aries-provision-dependencies:=resolve undergoes 
	 * the following state transitions when starting: INSTALLING -> INSTALLED 
	 * -> RESOLVING -> RESOLVED -> STARTING -> ACTIVE.
	 */
	@Test
	public void test5() throws Exception {
		Subsystem root = getRootSubsystem();
		subsystemEvents.clear();
		Subsystem subsystem = root.install(
				"application", 
				new SubsystemArchiveBuilder()
						.header(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, "application")
						.header(SubsystemConstants.SUBSYSTEM_TYPE, 
								SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' 
										+ AriesProvisionDependenciesDirective.RESOLVE.toString())
						.build()
		);
		try {
			long id = subsystem.getSubsystemId();
			assertEvent(subsystem, State.INSTALLING, subsystemEvents.poll(id, 5000));
			assertNull(subsystemEvents.poll(id, 1));
			subsystem.start();
			try {
				assertEvent(subsystem, State.INSTALLED, subsystemEvents.poll(id, 5000));
				assertEvent(subsystem, State.RESOLVING, subsystemEvents.poll(id, 5000));
				assertEvent(subsystem, State.RESOLVED, subsystemEvents.poll(id, 5000));
				assertEvent(subsystem, State.STARTING, subsystemEvents.poll(id, 5000));
				assertEvent(subsystem, State.ACTIVE, subsystemEvents.poll(id, 5000));
				assertNull(subsystemEvents.poll(id, 1));
			}
			finally {
				stopSubsystemSilently(subsystem);
			}
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	/*
	 * (6) Subsystem with apache-aries-provision-dependencies:=resolve has its 
	 * dependencies installed after a successful start.
	 */
	@Test
	public void test6() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem subsystem = root.install(
				"application", 
				new SubsystemArchiveBuilder()
						.symbolicName("application")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' 
										+ AriesProvisionDependenciesDirective.RESOLVE.toString())
						.content("bundle1")
						.bundle(
								"bundle1", 
								new BundleArchiveBuilder()
										.symbolicName("bundle1")
										.exportPackage("a")
										.importPackage("b")
										.build())
						.bundle(
								"bundle2", 
								new BundleArchiveBuilder()
										.symbolicName("bundle2")
										.exportPackage("b")
										.build())
						.build()
		);
		try {
			assertNotConstituent(root, "bundle2");
			startSubsystem(subsystem, false);
			try {
				assertConstituent(root, "bundle2");
			}
			finally {
				stopSubsystemSilently(subsystem);
			}
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	/*
	 * (7) Subsystem with apache-aries-provision-dependencies:=resolve is in the 
	 * INSTALL_FAILED state after an unsuccessful installation.
	 */
	@Test
	public void test7() throws Exception {
		Subsystem root = getRootSubsystem();
		subsystemEvents.clear();
		try {
			Subsystem subsystem = root.install(APPLICATION_INSTALL_FAILED, applicationInstallFailed());
			uninstallSubsystemSilently(subsystem);
			fail("Subsystem should not have installed");
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			long id = lastSubsystemId();
			assertEvent(id, APPLICATION_INSTALL_FAILED, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.INSTALLING, subsystemEvents.poll(id, 5000),
					ServiceEvent.REGISTERED);
			assertEvent(id, APPLICATION_INSTALL_FAILED, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.INSTALL_FAILED, subsystemEvents.poll(id, 5000),
					ServiceEvent.MODIFIED);
			assertEvent(id, APPLICATION_INSTALL_FAILED, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.UNINSTALLING, subsystemEvents.poll(id, 5000),
					ServiceEvent.MODIFIED);
			assertEvent(id, APPLICATION_INSTALL_FAILED, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.UNINSTALLED, subsystemEvents.poll(id, 5000),
					ServiceEvent.MODIFIED);
		}
	}
	
	/*
	 * (8) Subsystem with apache-aries-provision-dependencies:=resolve is not 
	 * available as a service after an unsuccessful installation.
	 */
	@Test
	public void test8() throws Exception {
		Subsystem root = getRootSubsystem();
		try {
			Subsystem subsystem = installSubsystem(root, APPLICATION_INSTALL_FAILED, applicationInstallFailed(), false);
			uninstallSubsystemSilently(subsystem);
			fail("Subsystem should not have installed");
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			assertEquals("Subsystem service should not exist", 0,
					bundleContext.getServiceReferences(
							Subsystem.class, 
							"(" + SubsystemConstants.SUBSYSTEM_ID_PROPERTY + "=" + lastSubsystemId() + ")"
					).size());
		}
	}
	
	/*
	 * (9) Subsystem with apache-aries-provision-dependencies:=resolve is in the 
	 * INSTALLING state when dependencies cannot be provisioned after invoking 
	 * the start method.
	 */
	@Test
	public void test9() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem subsystem = installSubsystem(root, APPLICATION_MISSING_DEPENDENCY, applicationMissingDependency(), false);
		try {
			startSubsystem(subsystem, false);
			stopSubsystemSilently(subsystem);
			fail("Subsystem should not have started");
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			assertState(State.INSTALLING, subsystem);
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	/*
	 * (10) Subsystem fails installation if the apache-aries-provision-dependencies 
	 * directive has a value other than "install" or "resolve".
	 */
	@Test
	public void test10() throws Exception {
		Subsystem root = getRootSubsystem();
		try {
			Subsystem subsystem = installSubsystem(
					root, 
					APPLICATION_INVALID_PROVISION_DEPENDENCIES, 
					applicationInvalidProvisionDependencies(), 
					false);
			uninstallSubsystemSilently(subsystem);
			fail("Subsystem should not have installed");
		}
		catch (SubsystemException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * (11) Subsystem with apache-aries-provision-dependencies:=resolve undergoes 
	 * the following state transitions when starting fails due to a runtime 
	 * resolution failure: INSTALLING -> INSTALLED -> RESOLVING -> INSTALLED.
	 */
	@Test
	public void test11() throws Exception {
		Subsystem root = getRootSubsystem();
		subsystemEvents.clear();
		Subsystem subsystem = root.install(APPLICATION_DEPENDENCY_IN_ARCHIVE, applicationDependencyInArchive());
		ServiceRegistration<ResolverHookFactory> registration = bundleContext.registerService(
				ResolverHookFactory.class, 
				new ResolverHookFactory() {
					@Override
					public ResolverHook begin(Collection<BundleRevision> triggers) {
						return new ResolverHook() {
							@Override
							public void filterResolvable(Collection<BundleRevision> candidates) {
								for (Iterator<BundleRevision> i = candidates.iterator(); i.hasNext();) {
									BundleRevision revision = i.next();
									if (revision.getSymbolicName().equals(BUNDLE_B)) {
										i.remove();
									}
								}
							}

							@Override
							public void filterSingletonCollisions(
									BundleCapability singleton,
									Collection<BundleCapability> collisionCandidates) {
								// Nothing.
							}

							@Override
							public void filterMatches(
									BundleRequirement requirement,
									Collection<BundleCapability> candidates) {
								// Nothing.
							}

							@Override
							public void end() {
								// Nothing.
							}
						};
					}
				}, 
				null
		);
		try {
			subsystem.start();
			stopSubsystemSilently(subsystem);
			fail("Subsystem should not have started");
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			long id = lastSubsystemId();
			assertEvent(id, APPLICATION_DEPENDENCY_IN_ARCHIVE, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.INSTALLING, subsystemEvents.poll(id, 5000),
					ServiceEvent.REGISTERED);
			assertEvent(id, APPLICATION_DEPENDENCY_IN_ARCHIVE, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.INSTALLED, subsystemEvents.poll(id, 5000),
					ServiceEvent.MODIFIED);
			assertEvent(id, APPLICATION_DEPENDENCY_IN_ARCHIVE, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.RESOLVING, subsystemEvents.poll(id, 5000),
					ServiceEvent.MODIFIED);
			assertEvent(id, APPLICATION_DEPENDENCY_IN_ARCHIVE, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.INSTALLED, subsystemEvents.poll(id, 5000),
					ServiceEvent.MODIFIED);
		}
		finally {
			registration.unregister();
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	/*
	 * (12) Subsystem with apache-aries-provision-dependencies:=resolve undergoes 
	 * the following state transitions when starting fails due to a start 
	 * failure: INSTALLING -> INSTALLED -> RESOLVING -> RESOLVED -> STARTING -> 
	 * RESOLVED.
	 */
	@Test
	public void test12() throws Exception {
		Subsystem root = getRootSubsystem();
		subsystemEvents.clear();
		Subsystem subsystem = root.install(APPLICATION_START_FAILURE, applicationStartFailure());
		try {
			subsystem.start();
			stopSubsystemSilently(subsystem);
			fail("Subsystem should not have started");
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			long id = lastSubsystemId();
			assertEvent(id, APPLICATION_START_FAILURE, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.INSTALLING, subsystemEvents.poll(id, 5000),
					ServiceEvent.REGISTERED);
			assertEvent(id, APPLICATION_START_FAILURE, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.INSTALLED, subsystemEvents.poll(id, 5000),
					ServiceEvent.MODIFIED);
			assertEvent(id, APPLICATION_START_FAILURE, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.RESOLVING, subsystemEvents.poll(id, 5000),
					ServiceEvent.MODIFIED);
			assertEvent(id, APPLICATION_START_FAILURE, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.RESOLVED, subsystemEvents.poll(id, 5000),
					ServiceEvent.MODIFIED);
			assertEvent(id, APPLICATION_START_FAILURE, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.STARTING, subsystemEvents.poll(id, 5000),
					ServiceEvent.MODIFIED);
			assertEvent(id, APPLICATION_START_FAILURE, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.RESOLVED, subsystemEvents.poll(id, 5000),
					ServiceEvent.MODIFIED);
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	/*
	 * (13) The root subsystem has apache-aries-provision-dependencies:=install.
	 */
	@Test
	public void test13() throws Exception {
		Subsystem root = getRootSubsystem();
		Map<String, String> headers = root.getSubsystemHeaders(null);
		String headerStr = headers.get(SubsystemConstants.SUBSYSTEM_TYPE);
		SubsystemTypeHeader header = new SubsystemTypeHeader(headerStr);
		AriesProvisionDependenciesDirective directive = header.getAriesProvisionDependenciesDirective();
		assertEquals(
				"Wrong directive", 
				AriesProvisionDependenciesDirective.INSTALL,
				directive);
	}
	
	/*
	 * (14) Subsystem with explicit apache-aries-provision-dependencies:=install 
	 * works as before.
	 */
	@Test
	public void test14() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem subsystem = installSubsystem(root, APPLICATION_PROVISION_DEPENDENCIES_INSTALL,
				applicationProvisionDependenciesInstall(), true);
		try {
			assertConstituent(subsystem, BUNDLE_A);
			assertConstituent(root, BUNDLE_B);
			startSubsystem(subsystem, true);
			stopSubsystem(subsystem);
		}
		finally {
			uninstallSubsystem(subsystem);
		}
	}
	
	/*
	 * (15) Unscoped subsystem with a value of apache-aries-provision-dependencies 
	 * that is different than the scoped parent fails installation.
	 */
	@Test
	public void test15a() throws Exception {
		Subsystem root = getRootSubsystem();
		try {
			Subsystem subsystem = installSubsystem(root, FEATURE_PROVISION_DEPENDENCIES_RESOLVE,
					featureProvisionDependenciesResolve(), false);
			uninstallSubsystemSilently(subsystem);
			fail("Subsystem should not have installed");
		}
		catch (SubsystemException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * (15) Unscoped subsystem with a value of apache-aries-provision-dependencies 
	 * that is different than the scoped parent fails installation.
	 */
	@Test
	public void test15b() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem application = installSubsystem(root, APPLICATION_PROVISION_DEPENDENCIES_INSTALL,
				applicationProvisionDependenciesInstall(), true);
		try {
			Subsystem feature = installSubsystem(application, FEATURE_PROVISION_DEPENDENCIES_RESOLVE,
					featureProvisionDependenciesResolve(), false);
			uninstallSubsystemSilently(feature);
			fail("Subsystem should not have installed");
		}
		catch (SubsystemException e) {
			e.printStackTrace();
		}
		finally {
			uninstallSubsystemSilently(application);
		}
	}
	
	/*
	 * (16) Unscoped subsystem with a value of apache-aries-provision-dependencies 
	 * that is the same as the scoped parent installs successfully.
	 */
	@Test
	public void test16a() throws Exception {
		Subsystem root = getRootSubsystem();
		try {
			Subsystem subsystem = installSubsystem(
					root, 
					FEATURE_PROVISION_DEPENDENCIES_INSTALL,
					new SubsystemArchiveBuilder()
							.symbolicName("application")
							.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' 
									+ AriesProvisionDependenciesDirective.INSTALL.toString())
							.build(), 
					true);
			uninstallSubsystemSilently(subsystem);
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			fail("Subsystem should have installed");
		}
	}
	
	/*
	 * (16) Unscoped subsystem with a value of apache-aries-provision-dependencies 
	 * that is the same as the scoped parent installs successfully.
	 */
	@Test
	public void test16b() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem application = installSubsystem(
				root, 
				"application",
				new SubsystemArchiveBuilder()
						.symbolicName("application")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' 
								+ AriesProvisionDependenciesDirective.INSTALL.toString())
						.build(), 
				true);
		try {
			Subsystem feature = installSubsystem(
					application, 
					"feature",
					new SubsystemArchiveBuilder()
							.symbolicName("feature")
							.type(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE + ';' 
									+ AriesProvisionDependenciesDirective.INSTALL.toString())
							.build(),  
					true);
			uninstallSubsystemSilently(feature);
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			fail("Subsystem should have installed");
		}
		finally {
			uninstallSubsystemSilently(application);
		}
	}
	
	/*
	 * (16) Unscoped subsystem with a value of apache-aries-provision-dependencies 
	 * that is the same as the scoped parent installs successfully.
	 */
	@Test
	public void test16c() throws Exception {
		Subsystem root = getRootSubsystem();
		try {
			Subsystem subsystem = installSubsystem(
					root, 
					"application",
					new SubsystemArchiveBuilder()
							.symbolicName("application")
							.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' 
									+ AriesProvisionDependenciesDirective.RESOLVE.toString())
							.subsystem(
									"feature", 
									new SubsystemArchiveBuilder()
											.symbolicName("feature")
											.type(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE + ';' 
													+ AriesProvisionDependenciesDirective.RESOLVE.toString())
											.build())
							.build(), 
					false);
			uninstallSubsystemSilently(subsystem);
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			fail("Subsystem should have installed");
		}
	}
	
	/*
	 * (17) Scoped subsystem with a value of apache-aries-provision-dependencies 
	 * that is the same as the scoped parent behaves accordingly.
	 */
	@Test
	public void test17() throws Exception {
		Subsystem root = getRootSubsystem();
		subsystemEvents.clear();
		Subsystem parent = root.install(APPLICATION_B, applicationB());
		try {
			long id = parent.getSubsystemId();
			assertEvent(id, APPLICATION_B, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.INSTALLING, subsystemEvents.poll(id, 5000),
					ServiceEvent.REGISTERED);
			assertNull("Unexpected event", subsystemEvents.poll(id, 1));
			assertNotConstituent(root, BUNDLE_B);
			assertConstituent(parent, BUNDLE_A);
			Subsystem child = getChild(parent, APPLICATION_A);
			assertNotNull("Missing child", child);
			id = child.getSubsystemId();
			assertEvent(id, APPLICATION_A, Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.INSTALLING, subsystemEvents.poll(id, 5000),
					ServiceEvent.REGISTERED);
			assertNull("Unexpected event", subsystemEvents.poll(id, 1));
			assertNotConstituent(root, BUNDLE_D);
			assertConstituent(child, BUNDLE_A);
			assertConstituent(child, BUNDLE_C);
			parent.start();
			try {
				id = parent.getSubsystemId();
				assertEvent(id, APPLICATION_B, Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.INSTALLED, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, APPLICATION_B, Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.RESOLVING, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, APPLICATION_B, Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.RESOLVED, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, APPLICATION_B, Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.STARTING, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, APPLICATION_B, Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.ACTIVE, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertNull("Unexpected event", subsystemEvents.poll(id, 1));
				assertConstituent(root, BUNDLE_B);
				id = child.getSubsystemId();
				assertEvent(id, APPLICATION_A, Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.INSTALLED, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, APPLICATION_A, Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.RESOLVING, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, APPLICATION_A, Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.RESOLVED, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, APPLICATION_A, Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.STARTING, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, APPLICATION_A, Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.ACTIVE, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertNull("Unexpected event", subsystemEvents.poll(id, 1));
				assertConstituent(root, BUNDLE_D);
			}
			finally {
				stopSubsystemSilently(parent);
			}
		}
		finally {
			uninstallSubsystemSilently(parent);
		}
	}
	
	/*
	 * (18) Scoped subsystem with a value of apache-aries-provision-dependencies 
	 * that overrides the scoped parent behaves accordingly.
	 */
	@Test
	public void test18() throws Exception {
		Subsystem root = getRootSubsystem();
		subsystemEvents.clear();
		Subsystem parent = root.install(
				"parent", 
				new SubsystemArchiveBuilder()
						.symbolicName("parent")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION 
								+ ';' 
								+ AriesProvisionDependenciesDirective.INSTALL.toString()
								+ ';'
								+ SubsystemConstants.PROVISION_POLICY_DIRECTIVE
								+ ":="
								+ SubsystemConstants.PROVISION_POLICY_ACCEPT_DEPENDENCIES)
						.content("bundle1")
						.bundle(
								"bundle1", 
								new BundleArchiveBuilder()
										.symbolicName("bundle1")
										.importPackage("a")
										.build())
						.bundle(
								"bundle2", 
								new BundleArchiveBuilder()
										.symbolicName("bundle2")
										.exportPackage("a")
										.build())
				.build()
		);
		try {
			long id = parent.getSubsystemId();
			assertEvent(id, "parent", Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.INSTALLING, subsystemEvents.poll(id, 5000),
					ServiceEvent.REGISTERED);
			assertEvent(id, "parent", Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.INSTALLED, subsystemEvents.poll(id, 5000),
					ServiceEvent.MODIFIED);
			assertNull("Unexpected event", subsystemEvents.poll(id, 1));
			assertConstituent(parent, "bundle2");
			assertConstituent(parent, "bundle1");
			parent.start();
			Subsystem child = parent.install(
					"child", 
					new SubsystemArchiveBuilder()
							.symbolicName("child")
							.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' 
									+ AriesProvisionDependenciesDirective.RESOLVE.toString())
							.content("bundle3")
							.bundle(
									"bundle3", 
									new BundleArchiveBuilder()
											.symbolicName("bundle3")
											.importPackage("b")
											.build())
							.bundle(
									"bundle4", 
									new BundleArchiveBuilder()
											.symbolicName("bundle4")
											.exportPackage("b")
											.build())
							.build()
			);
			id = child.getSubsystemId();
			assertEvent(id, "child", Version.emptyVersion,
					SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
					State.INSTALLING, subsystemEvents.poll(id, 5000),
					ServiceEvent.REGISTERED);
			assertNull("Unexpected event", subsystemEvents.poll(id, 1));
			assertNotConstituent(parent, "bundle4");
			assertConstituent(child, "bundle3");
			child.start();
			try {
				id = parent.getSubsystemId();
				assertEvent(id, "parent", Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.RESOLVING, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, "parent", Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.RESOLVED, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, "parent", Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.STARTING, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, "parent", Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.ACTIVE, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertNull("Unexpected event", subsystemEvents.poll(id, 1));
				id = child.getSubsystemId();
				assertEvent(id, "child", Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.INSTALLED, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, "child", Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.RESOLVING, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, "child", Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.RESOLVED, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, "child", Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.STARTING, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertEvent(id, "child", Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, 
						State.ACTIVE, subsystemEvents.poll(id, 5000),
						ServiceEvent.MODIFIED);
				assertNull("Unexpected event", subsystemEvents.poll(id, 1));
				assertConstituent(parent, "bundle4");
			}
			finally {
				stopSubsystemSilently(parent);
			}
		}
		finally {
			uninstallSubsystemSilently(parent);
		}
	}
	
	/*
	 * (19) Scoped subsystem with only features as parents is able to override 
	 * the value of apache-aries-provision-dependencies.
	 */
	@Test
	public void test19() throws Exception {
		Subsystem root = getRootSubsystem();
		subsystemEvents.clear();
		Subsystem feature1 = installSubsystem(
				root,
				"feature1", 
				new SubsystemArchiveBuilder()
						.symbolicName("feature1")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE)
						.build()
		);
		try {
			Subsystem feature2 = installSubsystem(
					root,
					"feature2", 
					new SubsystemArchiveBuilder()
							.symbolicName("feature2")
							.type(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE
									+ ';'
									+ AriesProvisionDependenciesDirective.INSTALL.toString())
							.build()
			);
			try {
				SubsystemArchiveBuilder applicationArchive = new SubsystemArchiveBuilder()
						.symbolicName("application")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION
								+ ';'
								+ AriesProvisionDependenciesDirective.RESOLVE.toString()
								+ ';'
								+ SubsystemConstants.PROVISION_POLICY_DIRECTIVE
								+ ":="
								+ SubsystemConstants.PROVISION_POLICY_ACCEPT_DEPENDENCIES)
						.content("bundle1")
						.bundle(
								"bundle1", 
								new BundleArchiveBuilder()
										.symbolicName("bundle1")
										.importPackage("a")
										.build())
						.bundle(
								"bundle2",
								new BundleArchiveBuilder()
										.symbolicName("bundle2")
										.exportPackage("a")
										.build());
				Subsystem application1 = feature1.install("application", applicationArchive.build());
				Subsystem application2 = feature2.install("application", applicationArchive.build());
				assertSame("Wrong subsystem", application1, application2);
				assertEquals("Wrong subsystem", application1, application2);
				assertChild(feature1, "application");
				assertChild(feature2, "application");
				long id = application1.getSubsystemId();
				assertEvent(id, "application", Version.emptyVersion,
						SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, State.INSTALLING, 
						subsystemEvents.poll(id, 5000), ServiceEvent.REGISTERED);
				assertNull("Unexpected event", subsystemEvents.poll(id, 1));
				assertConstituent(application1, "bundle1");
				assertNotConstituent(application1, "bundle2");
				application1.start();
				try {
					assertEvent(id, "application", Version.emptyVersion,
							SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, State.INSTALLED, 
							subsystemEvents.poll(id, 5000), ServiceEvent.MODIFIED);
					assertEvent(id, "application", Version.emptyVersion,
							SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, State.RESOLVING, 
							subsystemEvents.poll(id, 5000), ServiceEvent.MODIFIED);
					assertEvent(id, "application", Version.emptyVersion,
							SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, State.RESOLVED, 
							subsystemEvents.poll(id, 5000), ServiceEvent.MODIFIED);
					assertEvent(id, "application", Version.emptyVersion,
							SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, State.STARTING, 
							subsystemEvents.poll(id, 5000), ServiceEvent.MODIFIED);
					assertEvent(id, "application", Version.emptyVersion,
							SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, State.ACTIVE, 
							subsystemEvents.poll(id, 5000), ServiceEvent.MODIFIED);
					assertNull("Unexpected event", subsystemEvents.poll(id, 1));
					assertConstituent(application1, "bundle2");
				}
				finally {
					stopSubsystemSilently(application1);
				}
			}
			finally {
				uninstallSubsystemSilently(feature2);
			}
		}
		finally {
			uninstallSubsystemSilently(feature1);
		}
	}
	
	/*
	 * (20) Install a scoped subsystem, S1, with 
	 * apache-aries-provision-dependencies:=resolve. Install two features, F1 and 
	 * F2, independently as children of S1. F1 has bundle B1 as content. F2 has 
	 * bundle B2 as content. B2 has B1 as a dependency. B1 should be a 
	 * constituent of F1 but not of the root subsystem.
	 */
	@Test
	public void test20() throws Exception {
		serviceRegistrations.add(bundleContext.registerService(
				Repository.class,
				new TestRepository.Builder()
		        		.resource(new TestRepositoryContent.Builder()
		                		.capability(new TestCapability.Builder()
		                        		.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
		                        		.attribute(
		                        				IdentityNamespace.IDENTITY_NAMESPACE, 
		                        				"b1")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
		                        				IdentityNamespace.TYPE_BUNDLE))
		                        .content(new BundleArchiveBuilder()
		                        		.symbolicName("b1")
		                        		.exportPackage("a")
		                        		.buildAsBytes())
		                        .build())
		                .resource(new TestRepositoryContent.Builder()
		                		.capability(new TestCapability.Builder()
		                        		.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
		                        		.attribute(
		                        				IdentityNamespace.IDENTITY_NAMESPACE, 
		                        				"b2")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
		                        				IdentityNamespace.TYPE_BUNDLE))
		                        .content(new BundleArchiveBuilder()
		                        		.symbolicName("b2")
		                        		.importPackage("a")
		                        		.buildAsBytes())
		                        .build())
		        		.build(),
                null));
		Subsystem root = getRootSubsystem();
		Subsystem s1 = installSubsystem(
				root,
				"s1",
				new SubsystemArchiveBuilder()
						.symbolicName("s1")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION
								+ ';'
								+ AriesProvisionDependenciesDirective.RESOLVE.toString())
						.build(),
				false
		);
		uninstallableSubsystems.add(s1);
		startSubsystem(s1, false);
		stoppableSubsystems.add(s1);
		Subsystem f2 = installSubsystem(
				s1,
				"f2",
				new SubsystemArchiveBuilder()
						.symbolicName("f2")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE)
						.content("b2")
						.build(),
				false
		);
		uninstallableSubsystems.add(f2);
		assertChild(s1, "f2", null, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		assertConstituent(f2, "b2");
		Subsystem f1 = installSubsystem(
				s1,
				"f1",
				new SubsystemArchiveBuilder()
						.symbolicName("f1")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE)
						.content("b1")
						.build(),
				false
		);
		uninstallableSubsystems.add(f1);
		assertChild(s1, "f1", null, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		assertConstituent(f1, "b1");
		assertNotConstituent(root, "b1");
	}
	
	/*
	 * (21) Install a scoped subsystem, S1, with 
	 * apache-aries-provision-dependencies:=resolve. Install two features, F1 and 
	 * F2, independently as children of S1. F1 has bundle B1 and B2 as content. 
	 * F2 has bundle B2 and B3 as content. B2 is shared content. B1 has a 
	 * dependency on bundle B4, B2 has a dependency on bundle B5. B3 has a 
	 * dependency on bundle B6. Start F1. Dependency bundles B4 and B5 should be 
	 * provisioned but not B6.
	 */
	@Test
	public void test21() throws Exception {
		serviceRegistrations.add(bundleContext.registerService(
				Repository.class,
				new TestRepository.Builder()
		        		.resource(new TestRepositoryContent.Builder()
		                		.capability(new TestCapability.Builder()
		                        		.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
		                        		.attribute(
		                        				IdentityNamespace.IDENTITY_NAMESPACE, 
		                        				"b1")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
		                        				IdentityNamespace.TYPE_BUNDLE))
		                        .requirement(new TestRequirement.Builder()
		                        		.namespace(PackageNamespace.PACKAGE_NAMESPACE)
		                        		.directive(
		                        				PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE,
		                        				"(osgi.wiring.package=b4)"))
		                        .content(new BundleArchiveBuilder()
		                        		.symbolicName("b1")
		                        		.importPackage("b4")
		                        		.buildAsBytes())
		                        .build())
		                .resource(new TestRepositoryContent.Builder()
		                		.capability(new TestCapability.Builder()
		                        		.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
		                        		.attribute(
		                        				IdentityNamespace.IDENTITY_NAMESPACE, 
		                        				"b2")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
		                        				IdentityNamespace.TYPE_BUNDLE))
		                        .requirement(new TestRequirement.Builder()
		                        		.namespace(PackageNamespace.PACKAGE_NAMESPACE)
		                        		.directive(
		                        				PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE,
		                        				"(osgi.wiring.package=b5)"))
		                        .content(new BundleArchiveBuilder()
		                        		.symbolicName("b2")
		                        		.importPackage("b5")
		                        		.buildAsBytes())
		                        .build())
		                .resource(new TestRepositoryContent.Builder()
		                		.capability(new TestCapability.Builder()
		                        		.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
		                        		.attribute(
		                        				IdentityNamespace.IDENTITY_NAMESPACE, 
		                        				"b3")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
		                        				IdentityNamespace.TYPE_BUNDLE))
		                        .requirement(new TestRequirement.Builder()
		                        		.namespace(PackageNamespace.PACKAGE_NAMESPACE)
		                        		.directive(
		                        				PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE,
		                        				"(osgi.wiring.package=b6)"))
		                        .content(new BundleArchiveBuilder()
		                        		.symbolicName("b3")
		                        		.importPackage("b6")
		                        		.buildAsBytes())
		                        .build())
		                .resource(new TestRepositoryContent.Builder()
		                		.capability(new TestCapability.Builder()
		                        		.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
		                        		.attribute(
		                        				IdentityNamespace.IDENTITY_NAMESPACE, 
		                        				"b4")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
		                        				IdentityNamespace.TYPE_BUNDLE))
		                        .capability(new TestCapability.Builder()
		                        		.namespace(PackageNamespace.PACKAGE_NAMESPACE)
		                        		.attribute(
		                        				PackageNamespace.PACKAGE_NAMESPACE,
		                        				"b4"))
		                        .content(new BundleArchiveBuilder()
		                        		.symbolicName("b4")
		                        		.exportPackage("b4")
		                        		.buildAsBytes())
		                        .build())
		                .resource(new TestRepositoryContent.Builder()
		                		.capability(new TestCapability.Builder()
		                        		.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
		                        		.attribute(
		                        				IdentityNamespace.IDENTITY_NAMESPACE, 
		                        				"b5")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
		                        				IdentityNamespace.TYPE_BUNDLE))
		                        .capability(new TestCapability.Builder()
		                        		.namespace(PackageNamespace.PACKAGE_NAMESPACE)
		                        		.attribute(
		                        				PackageNamespace.PACKAGE_NAMESPACE,
		                        				"b5"))
		                        .content(new BundleArchiveBuilder()
		                        		.symbolicName("b5")
		                        		.exportPackage("b5")
		                        		.buildAsBytes())
		                        .build())
		                .resource(new TestRepositoryContent.Builder()
		                		.capability(new TestCapability.Builder()
		                        		.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
		                        		.attribute(
		                        				IdentityNamespace.IDENTITY_NAMESPACE, 
		                        				"b6")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
		                        				IdentityNamespace.TYPE_BUNDLE))
		                        .capability(new TestCapability.Builder()
		                        		.namespace(PackageNamespace.PACKAGE_NAMESPACE)
		                        		.attribute(
		                        				PackageNamespace.PACKAGE_NAMESPACE,
		                        				"b6"))
		                        .content(new BundleArchiveBuilder()
		                        		.symbolicName("b6")
		                        		.exportPackage("b6")
		                        		.buildAsBytes())
		                        .build())
		        		.build(),
                null));
		Subsystem root = getRootSubsystem();
		Subsystem s1 = installSubsystem(
				root,
				"s1",
				new SubsystemArchiveBuilder()
						.symbolicName("s1")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION
								+ ';'
								+ AriesProvisionDependenciesDirective.RESOLVE.toString()
								+ ';'
								+ SubsystemConstants.PROVISION_POLICY_DIRECTIVE
								+ ":="
								+ SubsystemConstants.PROVISION_POLICY_ACCEPT_DEPENDENCIES)
						.build(),
				false
		);
		uninstallableSubsystems.add(s1);
		startSubsystem(s1, false);
		stoppableSubsystems.add(s1);
		Subsystem f2 = installSubsystem(
				s1,
				"f2",
				new SubsystemArchiveBuilder()
				.symbolicName("f2")
				.type(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE)
				.content("b2,b3")
				.build(),
				false
				);
		uninstallableSubsystems.add(f2);
		assertChild(s1, "f2", null, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		assertConstituent(s1, "f2", null, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		assertConstituent(f2, "b2");
		assertConstituent(f2, "b3");
		Subsystem f1 = installSubsystem(
				s1,
				"f1",
				new SubsystemArchiveBuilder()
				.symbolicName("f1")
				.type(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE)
				.content("b1,b2")
				.build(),
				false
				);
		uninstallableSubsystems.add(f1);
		assertChild(s1, "f1", null, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		assertConstituent(s1, "f1", null, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		assertConstituent(f1, "b1");
		assertConstituent(f1, "b2");
		startSubsystem(f1, false);
		stoppableSubsystems.add(f1);
		assertState(State.RESOLVED, f2);
		assertConstituent(s1, "b4");
		assertConstituent(s1, "b5");
		assertConstituent(s1, "b6");
	}
	
	@Test
	public void testFullLifeCycle() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem subsystem = installSubsystem(root, APPLICATION_EMPTY, applicationEmpty(), false);
		startSubsystem(subsystem, false);
		stopSubsystem(subsystem);
		uninstallSubsystem(subsystem);
	}
	
	@Test
	public void testImplicitlyInstalledChildOverridesProvisionDependencies() throws Exception {
		Subsystem root = getRootSubsystem();
		subsystemEvents.clear();
		try {
			Subsystem subsystem = root.install(
					"parent", 
					new SubsystemArchiveBuilder()
							.symbolicName("parent")
							.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' +
									AriesProvisionDependenciesDirective.INSTALL.toString())
							.subsystem(
									"child", 
									new SubsystemArchiveBuilder()
											.symbolicName("child")
											.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' +
													AriesProvisionDependenciesDirective.RESOLVE.toString())
											.build())
							.build());
			uninstallSubsystemSilently(subsystem);
			fail("Subsystem should not have installed");
		}
		catch (SubsystemException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testInstall() throws Exception {
		Subsystem root = getRootSubsystem();
		try {
			Subsystem subsystem = installSubsystem(root, APPLICATION_EMPTY, applicationEmpty(), false);
			uninstallSubsystemSilently(subsystem);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Subsystem should have installed");
		}
	}
	
	@Test
	public void testInstallChildIntoInstallingParent() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem subsystem = installSubsystem(root, APPLICATION_DEPENDENCY_IN_ARCHIVE, applicationDependencyInArchive(), false);
		try {
			assertState(State.INSTALLING, subsystem);
			installSubsystem(subsystem, APPLICATION_A, applicationA(), false);
			fail("Subsystem should not have installed");
		}
		catch (SubsystemException e) {
			e.printStackTrace();
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	@Test
	public void testStart() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem subsystem = installSubsystem(root, APPLICATION_EMPTY, applicationEmpty(), false);
		try {
			startSubsystem(subsystem, false);
			stopSubsystemSilently(subsystem);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Subsystem should have started");
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	@Test
	public void testStop() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem subsystem = installSubsystem(root, APPLICATION_EMPTY, applicationEmpty(), false);
		try {
			startSubsystem(subsystem, false);
			try {
				stopSubsystem(subsystem);
			}
			catch (Exception e) {
				e.printStackTrace();
				fail("Subsystem should have stopped");
			}
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	}
	
	@Test
	public void testUninstall() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem subsystem = installSubsystem(root, APPLICATION_EMPTY, applicationEmpty(), false);
		try {
			uninstallSubsystem(subsystem);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Subsystem should have uninstalled");
		}
	}
	
	private InputStream applicationA() throws Exception {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
		attributes.putValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		attributes.putValue(SubsystemConstants.SUBSYSTEM_TYPE, 
				SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' +
				AriesProvisionDependenciesDirective.RESOLVE.toString());
		attributes.putValue(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A + ',' + BUNDLE_C);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		manifest.write(baos);
		baos.close();
		return TinyBundles
				.bundle()
				.add(SUBSYSTEM_MANIFEST_FILE, new ByteArrayInputStream(baos.toByteArray()))
				.add(BUNDLE_A + JAR_EXTENSION, bundleA())
				.add(BUNDLE_B + JAR_EXTENSION, bundleB())
				.add(BUNDLE_C + JAR_EXTENSION, bundleC())
				.add(BUNDLE_D + JAR_EXTENSION, bundleD())
				.build();
	}
	
	private InputStream applicationB() throws Exception {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
		attributes.putValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_B);
		attributes.putValue(SubsystemConstants.SUBSYSTEM_TYPE, 
				SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' +
				AriesProvisionDependenciesDirective.RESOLVE.toString());
		attributes.putValue(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A + ',' + APPLICATION_A + ";type=osgi.subsystem.application");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		manifest.write(baos);
		baos.close();
		return TinyBundles
				.bundle()
				.add(SUBSYSTEM_MANIFEST_FILE, new ByteArrayInputStream(baos.toByteArray()))
				.add(BUNDLE_A + JAR_EXTENSION, bundleA())
				.add(BUNDLE_B + JAR_EXTENSION, bundleB())
				.add(APPLICATION_A + ESA_EXTENSION, applicationA())
				.build();
	}
	
	private InputStream applicationDependencyInArchive() throws Exception {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
		attributes.putValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_DEPENDENCY_IN_ARCHIVE);
		attributes.putValue(SubsystemConstants.SUBSYSTEM_TYPE, 
				SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' +
				AriesProvisionDependenciesDirective.RESOLVE.toString());
		attributes.putValue(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		manifest.write(baos);
		baos.close();
		return TinyBundles
				.bundle()
				.add(SUBSYSTEM_MANIFEST_FILE, new ByteArrayInputStream(baos.toByteArray()))
				.add(BUNDLE_A + JAR_EXTENSION, bundleA())
				.add(BUNDLE_B + JAR_EXTENSION, bundleB())
				.build();
	}
	
	private InputStream applicationEmpty() throws Exception {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), MANIFEST_VERSION);
		attributes.putValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_EMPTY);
		attributes.putValue(SubsystemConstants.SUBSYSTEM_TYPE, 
				SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' +
				AriesProvisionDependenciesDirective.RESOLVE.toString());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		manifest.write(baos);
		baos.close();
		return TinyBundles
				.bundle()
				.add(SUBSYSTEM_MANIFEST_FILE, new ByteArrayInputStream(baos.toByteArray()))
				.build();
	}
	
	private InputStream applicationInstallFailed() throws Exception {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), MANIFEST_VERSION);
		attributes.putValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_INSTALL_FAILED);
		attributes.putValue(SubsystemConstants.SUBSYSTEM_TYPE, 
				SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' +
				AriesProvisionDependenciesDirective.RESOLVE.toString());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		manifest.write(baos);
		baos.close();
		return TinyBundles
				.bundle()
				.add(SUBSYSTEM_MANIFEST_FILE, new ByteArrayInputStream(baos.toByteArray()))
				.add(BUNDLE_INVALID_MANIFEST + JAR_EXTENSION, bundleInvalidManifest())
				.build();
	}
	
	private InputStream applicationInvalidProvisionDependencies() throws Exception {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
		attributes.putValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_INVALID_PROVISION_DEPENDENCIES);
		attributes.putValue(SubsystemConstants.SUBSYSTEM_TYPE, 
				SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' +
				AriesProvisionDependenciesDirective.NAME + ":=foo");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		manifest.write(baos);
		baos.close();
		return TinyBundles
				.bundle()
				.add(SUBSYSTEM_MANIFEST_FILE, new ByteArrayInputStream(baos.toByteArray()))
				.build();
	}
	
	private InputStream applicationMissingDependency() throws Exception {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
		attributes.putValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_MISSING_DEPENDENCY);
		attributes.putValue(SubsystemConstants.SUBSYSTEM_TYPE, 
				SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' +
				AriesProvisionDependenciesDirective.RESOLVE.toString());
		attributes.putValue(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		manifest.write(baos);
		baos.close();
		return TinyBundles
				.bundle()
				.add(SUBSYSTEM_MANIFEST_FILE, new ByteArrayInputStream(baos.toByteArray()))
				.add(BUNDLE_A + JAR_EXTENSION, bundleA())
				.build();
	}
	
	private InputStream applicationProvisionDependenciesInstall() throws Exception {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
		attributes.putValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_PROVISION_DEPENDENCIES_INSTALL);
		attributes.putValue(SubsystemConstants.SUBSYSTEM_TYPE, 
				SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' +
				AriesProvisionDependenciesDirective.INSTALL.toString());
		attributes.putValue(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		manifest.write(baos);
		baos.close();
		return TinyBundles
				.bundle()
				.add(SUBSYSTEM_MANIFEST_FILE, new ByteArrayInputStream(baos.toByteArray()))
				.add(BUNDLE_A + JAR_EXTENSION, bundleA())
				.add(BUNDLE_B + JAR_EXTENSION, bundleB())
				.build();
	}
	
	private InputStream applicationStartFailure() throws Exception {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
		attributes.putValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_START_FAILURE);
		attributes.putValue(SubsystemConstants.SUBSYSTEM_TYPE, 
				SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' +
				AriesProvisionDependenciesDirective.RESOLVE.toString());
		attributes.putValue(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_START_FAILURE);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		manifest.write(baos);
		baos.close();
		return TinyBundles
				.bundle()
				.add(SUBSYSTEM_MANIFEST_FILE, new ByteArrayInputStream(baos.toByteArray()))
				.add(BUNDLE_START_FAILURE + JAR_EXTENSION, bundleStartFailure())
				.build();
	}
	
	private void assertReferences(Class<?> clazz, String filter, int expected) throws Exception {
		ServiceReference<?>[] references = bundleContext.getAllServiceReferences(clazz.getName(), filter);
		if (expected < 1) {
			assertNull("References exist", references);
		}
		else {
			assertNotNull("No references", references);
			assertEquals("No references or more than one", expected, references.length);
		}
	}

	private InputStream bundleA() {
		return TinyBundles
				.bundle()
				.set(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_A)
				.set(Constants.EXPORT_PACKAGE, PACKAGE_A)
				.set(Constants.IMPORT_PACKAGE, PACKAGE_B)
				.build();
	}
	
	private InputStream bundleB() {
		return TinyBundles
				.bundle()
				.set(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_B)
				.set(Constants.EXPORT_PACKAGE, PACKAGE_B)
				.build();
	}
	
	private InputStream bundleC() {
		return TinyBundles
				.bundle()
				.set(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_C)
				.set(Constants.EXPORT_PACKAGE, PACKAGE_C)
				.set(Constants.IMPORT_PACKAGE, PACKAGE_B)
				.set(Constants.IMPORT_PACKAGE, PACKAGE_D)
				.build();
	}
	
	private InputStream bundleD() {
		return TinyBundles
				.bundle()
				.set(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_D)
				.set(Constants.EXPORT_PACKAGE, PACKAGE_D)
				.build();
	}
	
	private InputStream bundleInvalidManifest() {
		return TinyBundles
				.bundle()
				.set(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_INVALID_MANIFEST)
				.set(Constants.PROVIDE_CAPABILITY, "osgi.ee;osgi.ee=J2SE-1.4")
				.build();
	}
	
	private InputStream bundleStartFailure() {
		return TinyBundles
				.bundle()
				.set(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_START_FAILURE)
				.set(Constants.BUNDLE_ACTIVATOR, BundleStartFailureActivator.class.getName())
				.add(BundleStartFailureActivator.class, InnerClassStrategy.NONE)
				.build();
	}
	
	private InputStream featureProvisionDependenciesResolve() throws Exception {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
		attributes.putValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_PROVISION_DEPENDENCIES_RESOLVE);
		attributes.putValue(SubsystemConstants.SUBSYSTEM_TYPE, 
				SubsystemConstants.SUBSYSTEM_TYPE_FEATURE + ';' +
				AriesProvisionDependenciesDirective.RESOLVE.toString());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		manifest.write(baos);
		baos.close();
		return TinyBundles
				.bundle()
				.add(SUBSYSTEM_MANIFEST_FILE, new ByteArrayInputStream(baos.toByteArray()))
				.build();
	}
	
	private static class BundleStartFailureActivator implements BundleActivator {
		@Override
		public void start(BundleContext context) throws Exception {
			throw new IllegalStateException();
		}

		@Override
		public void stop(BundleContext context) throws Exception {
			// Nothing.
		}
	}
	
	@Override
    public void setUp() throws Exception {
        super.setUp();
        stoppableSubsystems.clear();
        uninstallableSubsystems.clear();
    }
	
	@Override
    public void tearDown() throws Exception {
		for (Subsystem subsystem : stoppableSubsystems) {
			stopSubsystemSilently(subsystem);
		}
		for (Subsystem subsystem : uninstallableSubsystems) {
			uninstallSubsystemSilently(subsystem);
		}
        super.tearDown();
    }
	
	@Test
	public void testInterleavingContentDependencies() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem c1 = installSubsystem(
				root,
				"c1",
				new SubsystemArchiveBuilder()
						.symbolicName("c1")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE
								+ ';'
								+ AriesProvisionDependenciesDirective.RESOLVE.toString())
						.content("c1b1;version=\"[0,0]\"")
						.exportPackage("c1b1")
						.importPackage("c2b1")
						.bundle(
								"c1b1", 
								new BundleArchiveBuilder()
										.symbolicName("c1b1")
										.exportPackage("c1b1")
										.importPackage("c2b1")
										.build())
						.build(),
				false
		);
		uninstallableSubsystems.add(c1);
		Subsystem c2 = installSubsystem(
				root,
				"c2",
				new SubsystemArchiveBuilder()
						.symbolicName("c2")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE
								+ ';'
								+ AriesProvisionDependenciesDirective.RESOLVE.toString())
						.content("c2b1;version=\"[0,0]\"")
						.exportPackage("c2b1")
						.importPackage("c1b1")
						.bundle(
								"c2b1", 
								new BundleArchiveBuilder()
										.symbolicName("c2b1")
										.exportPackage("c2b1")
										.importPackage("c1b1")
										.build())
						.build(),
				false
		);
		uninstallableSubsystems.add(c2);
		startSubsystem(c1, false);
		stoppableSubsystems.add(c1);
		assertState(EnumSet.of(State.RESOLVED, State.ACTIVE), c2);
	}
	
	@Test
	public void testRestart2() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem c1 = installSubsystem(
				root,
				"c1",
				new SubsystemArchiveBuilder()
						.symbolicName("c1")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE
								+ ';'
								+ AriesProvisionDependenciesDirective.RESOLVE.toString())
						.content("c1b1;version=\"[0,0]\"")
						.exportPackage("c1b1")
						.importPackage("c2b1")
						.bundle(
								"c1b1", 
								new BundleArchiveBuilder()
										.symbolicName("c1b1")
										.exportPackage("c1b1")
										.importPackage("c2b1")
										.build())
						.build(),
				false
		);
		Subsystem c2 = installSubsystem(
				root,
				"c2",
				new SubsystemArchiveBuilder()
						.symbolicName("c2")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE
								+ ';'
								+ AriesProvisionDependenciesDirective.RESOLVE.toString())
						.content("c2b1;version=\"[0,0]\"")
						.exportPackage("c2b1")
						.importPackage("c1b1")
						.bundle(
								"c2b1", 
								new BundleArchiveBuilder()
										.symbolicName("c2b1")
										.exportPackage("c2b1")
										.importPackage("c1b1")
										.build())
						.build(),
				false
		);
		assertChild(root, "c1", null, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		assertChild(root, "c2", null, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		restartSubsystemsImplBundle();
		root = getRootSubsystem();
		c1 = getChild(root, "c1", null, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		assertNotNull("Missing child", c1);
		uninstallableSubsystems.add(c1);
		c2 = getChild(root, "c2", null, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		assertNotNull("Missing child", c2);
		uninstallableSubsystems.add(c2);
		startSubsystem(c1, false);
		stoppableSubsystems.add(c1);
		try {
			assertState(EnumSet.of(State.RESOLVED, State.ACTIVE), c2);
		}
		catch (AssertionError e) {
			System.out.println(c2.getState());
		}
	}
	
	@Test
	public void testRestart() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem a1 = installSubsystem(
				root,
				"a1", 
				new SubsystemArchiveBuilder()
						.symbolicName("a1")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' 
								+ AriesProvisionDependenciesDirective.RESOLVE.toString())
						.content("b1,c1;type=osgi.subsystem.composite")
						.bundle(
								"b1", 
								new BundleArchiveBuilder()
										.symbolicName("b1")
										.importPackage("b2")
										.build())
						.bundle(
								"b2", 
								new BundleArchiveBuilder()
										.symbolicName("b2")
										.exportPackage("b2")
										.build())
						.subsystem(
								"c1", 
								new SubsystemArchiveBuilder()
										.symbolicName("c1")
										.type(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE
												+ ';'
												+ AriesProvisionDependenciesDirective.RESOLVE.toString())
										.content("b1;version=\"[0,0]\"")
										.importPackage("b2")
										.bundle(
												"b1", 
												new BundleArchiveBuilder()
														.symbolicName("b1")
														.importPackage("b2")
														.build())
								.build())
						.build(),
				false);
		uninstallableSubsystems.add(a1);
		assertChild(root, "a1");
		assertState(State.INSTALLING, a1);
		Subsystem c1 = getChild(a1, "c1", null, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		assertNotNull("Missing child", c1);
		assertState(State.INSTALLING, c1);

		restartSubsystemsImplBundle();
		root = getRootSubsystem();
		
		a1 = getChild(root, "a1");
		assertNotNull("Missing child", a1);
		uninstallableSubsystems.add(a1);
		assertState(State.INSTALLING, a1);
		assertConstituent(a1, "b1");
		assertNotConstituent(root, "b2");
		
		c1 = getChild(a1, "c1", null, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		assertNotNull("Missing child", c1);
		uninstallableSubsystems.add(c1);
		assertConstituent(c1, "b1");
		
		startSubsystem(c1, false);
		stoppableSubsystems.add(c1);
		
		assertState(State.INSTALLED, a1);
		stoppableSubsystems.add(a1);
		
		assertConstituent(root, "b2");
	}
	
	@Test
	public void test4e3bCompliance() throws Exception {
		serviceRegistrations.add(bundleContext.registerService(
				Repository.class,
				new TestRepository.Builder()
		        		.resource(new TestRepositoryContent.Builder()
		                		.capability(new TestCapability.Builder()
		                        		.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
		                        		.attribute(
		                        				IdentityNamespace.IDENTITY_NAMESPACE, 
		                        				"a")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
		                        				IdentityNamespace.TYPE_BUNDLE))
		                        .capability(new TestCapability.Builder()
		                        		.namespace(PackageNamespace.PACKAGE_NAMESPACE)
		                        		.attribute(
		                        				PackageNamespace.PACKAGE_NAMESPACE, 
		                        				"x")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion))
		                        .capability(new TestCapability.Builder()
		                        		.namespace(BundleNamespace.BUNDLE_NAMESPACE)
		                        		.attribute(
		                        				BundleNamespace.BUNDLE_NAMESPACE, 
		                        				"a")
		                        		.attribute(
		                        				BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion))
		                        .content(new BundleArchiveBuilder()
		                        		.symbolicName("a")
		                        		.exportPackage("x")
		                        		.buildAsBytes())
		                        .build())
		                .resource(new TestRepositoryContent.Builder()
		                		.capability(new TestCapability.Builder()
		                        		.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
		                        		.attribute(
		                        				IdentityNamespace.IDENTITY_NAMESPACE, 
		                        				"b")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
		                        				IdentityNamespace.TYPE_BUNDLE))
		                        .capability(new TestCapability.Builder()
		                        		.namespace("y"))
		                        .content(new BundleArchiveBuilder()
		                        		.symbolicName("b")
		                        		.header("Provide-Capability", "y")
		                        		.buildAsBytes())
		                        .build())
		        		.build(),
                null));
		Subsystem root = getRootSubsystem();
		try {
			Subsystem s1 = installSubsystem(
					root,
					"s1",
					new SubsystemArchiveBuilder()
							.symbolicName("s1")
							.type(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE)
							.subsystem(
									"s3",
									new SubsystemArchiveBuilder()
											.symbolicName("s3")
											.type(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE)
											.subsystem(
													"s2", 
													new SubsystemArchiveBuilder()
															.symbolicName("s2")
															.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION)
															.bundle(
																	"c", 
																	new BundleArchiveBuilder()
																			.symbolicName("c")
																			.importPackage("x")
																			.build())
															.bundle(
																	"d", 
																	new BundleArchiveBuilder()
																			.symbolicName("d")
																			.header("Require-Bundle", "a")
																			.build())
															.bundle(
																	"e", 
																	new BundleArchiveBuilder()
																			.symbolicName("e")
																			.header("Require-Capability", "y")
																			.build())
															.build())
											.build())
							.build(),
					true);
			uninstallableSubsystems.add(s1);
			fail("Subsystem should not have installed");
		}
		catch (SubsystemException e) {
			e.printStackTrace();
		}
	}
}
