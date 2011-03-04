package org.apache.aries.subsystem.scope.itests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class AbstractTest extends AbstractIntegrationTest {
	protected Scope scope;
	
	private ServiceReference<Scope> scopeRef;
	
	protected void addPackageExportPolicy(String packageName, ScopeUpdate scopeUpdate) throws InvalidSyntaxException {
		Filter filter = bundleContext.createFilter("(osgi.wiring.package=" + packageName + ')');
		SharePolicy policy = new SharePolicy(SharePolicy.TYPE_EXPORT, "osgi.wiring.package", filter);
		Map<String, List<SharePolicy>> policyMap = scopeUpdate.getSharePolicies(SharePolicy.TYPE_EXPORT);
		List<SharePolicy> policies = policyMap.get("osgi.wiring.package");
		if (policies == null) {
			policies = new ArrayList<SharePolicy>();
			policyMap.put("osgi.wiring.package", policies);
		}
		policies.add(policy);
	}
	
	protected void addPackageImportPolicy(String packageName, ScopeUpdate scopeUpdate) throws InvalidSyntaxException {
		Filter filter = bundleContext.createFilter("(osgi.wiring.package=" + packageName + ')');
		SharePolicy policy = new SharePolicy(SharePolicy.TYPE_IMPORT, "osgi.wiring.package", filter);
		Map<String, List<SharePolicy>> policyMap = scopeUpdate.getSharePolicies(SharePolicy.TYPE_IMPORT);
		List<SharePolicy> policies = policyMap.get("osgi.wiring.package");
		if (policies == null) {
			policies = new ArrayList<SharePolicy>();
			policyMap.put("osgi.wiring.package", policies);
		}
		policies.add(policy);
	}
	
	protected void addServiceExportPolicy(Class<?> clazz, ScopeUpdate scopeUpdate) throws InvalidSyntaxException {
		Filter filter = bundleContext.createFilter('(' + Constants.OBJECTCLASS + '=' + clazz.getName() + ')');
		SharePolicy policy = new SharePolicy(SharePolicy.TYPE_EXPORT, "scope.share.service", filter);
		Map<String, List<SharePolicy>> policyMap = scopeUpdate.getSharePolicies(SharePolicy.TYPE_EXPORT);
		List<SharePolicy> policies = policyMap.get("scope.share.service");
		if (policies == null) {
			policies = new ArrayList<SharePolicy>();
			policyMap.put("scope.share.service", policies);
		}
		policies.add(policy);
	}
	
	protected void addServiceImportPolicy(Class<?> clazz, ScopeUpdate scopeUpdate) throws InvalidSyntaxException {
		Filter filter = bundleContext.createFilter('(' + Constants.OBJECTCLASS + '=' + clazz.getName() + ')');
		SharePolicy policy = new SharePolicy(SharePolicy.TYPE_IMPORT, "scope.share.service", filter);
		Map<String, List<SharePolicy>> policyMap = scopeUpdate.getSharePolicies(SharePolicy.TYPE_IMPORT);
		List<SharePolicy> policies = policyMap.get("scope.share.service");
		if (policies == null) {
			policies = new ArrayList<SharePolicy>();
			policyMap.put("scope.share.service", policies);
		}
		policies.add(policy);
	}
	
	protected void assertEmpty(Collection<?> c) {
		assertNotNull(c);
		assertTrue(c.isEmpty());
	}
	
	protected void assertEmpty(Map<?, ?> m) {
		assertNotNull(m);
		assertTrue(m.isEmpty());
	}
	
	protected void assertCollectionEquals(Collection<?> c1, Collection<?> c2) {
		assertFalse((c1 == null && c2 != null) || (c1 != null && c2 == null));
		assertTrue(c1.size() == c2.size());
		for (Iterator<?> i = c2.iterator(); i.hasNext();) {
			assertTrue(c2.contains(i.next()));
		}
	}
	
	protected Bundle findBundle(String symbolicName, Scope scope) {
		return Utils.findBundle(symbolicName, scope);
	}
	
	protected Bundle findBundleInRootScope(String symbolicName) {
		return findBundle(symbolicName, scope);
	}
	
	protected ScopeUpdate findChildUpdate(String name, ScopeUpdate parent) {
		assertNotNull(name);
		assertNotNull(parent);
		ScopeUpdate result = null;
		for (ScopeUpdate child : parent.getChildren()) {
			if (name.equals(child.getName())) {
				result = child;
				break;
			}
		}
		assertNotNull(result);
		return result;
	}
	
	protected String getBundleLocation(String bundle) {
		URL url = AbstractTest.class.getClassLoader().getResource(bundle);
		return url.toExternalForm();
	}
	
	protected Bundle installBundle(String name) throws BundleException {
		URL url = AbstractTest.class.getClassLoader().getResource(name);
		return bundleContext.installBundle(url.toExternalForm());
	}
	
	protected void installBundles(Scope scope, String[] bundleNames) throws Exception {
		installBundles(scope, Arrays.asList(bundleNames));
	}
	
	protected void installBundles(Scope scope, Collection<String> bundleNames) throws Exception {
		ScopeUpdate scopeUpdate = scope.newScopeUpdate();
		for (String bundleName : bundleNames) {
			URL url = AbstractTest.class.getClassLoader().getResource(bundleName);
			InstallInfo installInfo = new InstallInfo(url.toExternalForm(), url.openStream());
			scopeUpdate.getBundlesToInstall().add(installInfo);
		}
		scopeUpdate.commit();
	}
	
	@Before
	public void before() throws Exception {
		assertNotNull(bundleContext);
		scopeRef = bundleContext.getServiceReference(Scope.class);
		assertNotNull(scopeRef);
		scope = bundleContext.getService(scopeRef);
		assertNotNull(scope);
	}

	@After
	public void after() throws Exception {
	}
	
	protected void uninstallQuietly(Bundle bundle) {
		Utils.uninstallQuietly(bundle);
	}
	
	@org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        Option[] options = options(
            // Log
            mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
            mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
            // Felix Config Admin
            mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
            // Felix mvn url handler
            mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),


            // this is how you set the default log level when using pax logging (logProfile)
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),

            // Bundles
            mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
            mavenBundle("org.apache.aries", "org.apache.aries.util"),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
            mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository"),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.api"),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.scope.api"),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.scope.impl"),

            // org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),

            PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),

            equinox().version("3.7.0.v20110221"),
            
            new Customizer() {
            	@Override
                public InputStream customizeTestProbe(InputStream testProbe) throws IOException {
                    return TinyBundles.modifyBundle(testProbe).
                                      removeHeader(Constants.EXPORT_PACKAGE)
                                      .set(Constants.EXPORT_PACKAGE, "org.apache.aries.subsystem.scope.itests")
                                      .build();
                }
            }
        );
        options = updateOptions(options);
        return options;
    }
}
