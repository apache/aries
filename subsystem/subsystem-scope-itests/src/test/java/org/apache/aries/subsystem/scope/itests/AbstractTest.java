package org.apache.aries.subsystem.scope.itests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public abstract class AbstractTest extends AbstractIntegrationTest {
	@Inject
	Scope scope;
	
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
	
	protected Bundle findBundle(String symbolicName) {
		return Utils.findBundle(symbolicName, bundleContext);
	}
	
	protected Bundle findBundle(String symbolicName, Scope scope) {
		return Utils.findBundle(symbolicName, scope);
	}
	
	protected Bundle findBundleInRootScope(String symbolicName) {
		return findBundle(symbolicName, getScope());
	}
	
	protected Scope findChildScope(String name, Scope parent) {
		assertNotNull(name);
		assertNotNull(parent);
		Scope result = null;
		for (Scope child : parent.getChildren()) {
			if (name.equals(child.getName())) {
				result = child;
				break;
			}
		}
		assertNotNull(result);
		return result;
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
	
	protected Scope getScope() {
		return scope;
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
	
	protected void uninstallQuietly(Bundle bundle) {
		Utils.uninstallQuietly(bundle);
	}
	
	protected Option baseOptions() {
        String localRepo = System.getProperty("maven.repo.local");
     
        if (localRepo == null) {
            localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
        }
        return composite(
                junitBundles(),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-api", "1.7.2"),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.7.2"),
                mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit").versionAsInProject(),
                // this is how you set the default log level when using pax
                // logging (logProfile)
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
                when(localRepo != null).useOptions(vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo))
         );
    }
	
	@ProbeBuilder
	public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
		probe.setHeader(Constants.EXPORT_PACKAGE, this.getClass().getPackage().getName());
		return probe;
	}
	
	@Configuration
    public Option[] subsystemScope() {
        //InputStream itestBundle = TinyBundles.bundle().add();
		return CoreOptions.options(
        		baseOptions(),
            mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit").versionAsInProject(),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.api").versionAsInProject(),
            mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils").versionAsInProject(),
            mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository").versionAsInProject(),
            mavenBundle("org.eclipse.equinox", "org.eclipse.equinox.coordinator").versionAsInProject(),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.api").versionAsInProject(),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.scope.api").versionAsInProject(),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.scope.impl").versionAsInProject()
            //CoreOptions.streamBundle(itestBundle )
        );
    }
}
