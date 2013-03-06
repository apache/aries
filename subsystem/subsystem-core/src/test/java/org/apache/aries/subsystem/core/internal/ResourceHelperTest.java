package org.apache.aries.subsystem.core.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class ResourceHelperTest {
	@Test
	public void testMandatoryDirectiveAbsent() {
		Capability cap = new BasicCapability.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.attribute(PackageNamespace.PACKAGE_NAMESPACE, "com.foo")
				.attribute("a", "b")
				.attribute("b", "c")
				.attribute("c", "d")
				.resource(EasyMock.createNiceMock(Resource.class)).build();
		Requirement req = new BasicRequirement.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.directive(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE,
						"(&(osgi.wiring.package=com.foo)(a=b)(b=c))")
				.resource(EasyMock.createNiceMock(Resource.class)).build();
		assertTrue("Capability should match requirement", ResourceHelper.matches(req, cap));
	}
	
	@Test
	public void testMandatoryDirectiveAndNullFilterDirective() {
		Capability cap = new BasicCapability.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.attribute(PackageNamespace.PACKAGE_NAMESPACE, "com.foo")
				.attribute("a", "b")
				.attribute("b", "c")
				.attribute("c", "d")
				.directive(PackageNamespace.CAPABILITY_MANDATORY_DIRECTIVE, "b")
				.resource(EasyMock.createNiceMock(Resource.class)).build();
		Requirement req = new BasicRequirement.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.resource(EasyMock.createNiceMock(Resource.class)).build();
		assertFalse("Capability should not match requirement", ResourceHelper.matches(req, cap));
	}
	
	@Test
	public void testMandatoryDirectiveCaseSensitive() {
		Capability cap = new BasicCapability.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.attribute(PackageNamespace.PACKAGE_NAMESPACE, "com.foo")
				.attribute("a", "b")
				.attribute("bAr", "c")
				.attribute("c", "d")
				.directive(PackageNamespace.CAPABILITY_MANDATORY_DIRECTIVE, "bAr")
				.resource(EasyMock.createNiceMock(Resource.class)).build();
		Requirement req = new BasicRequirement.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.directive(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE,
						"(&(osgi.wiring.package=com.foo)(a=b)(baR=c)(c=d))")
				.resource(EasyMock.createNiceMock(Resource.class)).build();
		assertFalse("Capability should not match requirement", ResourceHelper.matches(req, cap));
	}
	
	@Test
	public void testMandatoryDirectiveExportPackageFail() {
		Capability cap = new BasicCapability.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.attribute(PackageNamespace.PACKAGE_NAMESPACE, "com.foo")
				.attribute("a", "b")
				.attribute("b", "c")
				.attribute("c", "d")
				.directive(PackageNamespace.CAPABILITY_MANDATORY_DIRECTIVE, "a,c")
				.resource(EasyMock.createNiceMock(Resource.class))
				.build();
		Requirement req = new BasicRequirement.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.directive(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE,
						"(&(osgi.wiring.package=com.foo)(a=b)(b=c))")
				.resource(EasyMock.createNiceMock(Resource.class)).build();
		assertFalse("Capability should not match requirement", ResourceHelper.matches(req, cap));
	}
	
	@Test
	public void testMandatoryDirectiveExportPackagePass() {
		Capability cap = new BasicCapability.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.attribute(PackageNamespace.PACKAGE_NAMESPACE, "com.foo")
				.attribute("a", "b")
				.attribute("b", "c")
				.attribute("c", "d")
				.directive(PackageNamespace.CAPABILITY_MANDATORY_DIRECTIVE, "a,c")
				.resource(EasyMock.createNiceMock(Resource.class))
				.build();
		Requirement req = new BasicRequirement.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.directive(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE,
						"(&(osgi.wiring.package=com.foo)(a=b)(c=d))")
				.resource(EasyMock.createNiceMock(Resource.class)).build();
		assertTrue("Capability should match requirement", ResourceHelper.matches(req, cap));
	}
	
	@Test
	public void testMandatoryDirectiveWithWhitespace() {
		Capability cap = new BasicCapability.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.attribute(PackageNamespace.PACKAGE_NAMESPACE, "com.foo")
				.attribute("a", "b")
				.attribute("b", "c")
				.attribute("c", "d")
				.directive(PackageNamespace.CAPABILITY_MANDATORY_DIRECTIVE, "\ra\n, c	")
				.resource(EasyMock.createNiceMock(Resource.class))
				.build();
		Requirement req = new BasicRequirement.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.directive(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE,
						"(&(osgi.wiring.package=com.foo)(a=b)(c=d))")
				.resource(EasyMock.createNiceMock(Resource.class)).build();
		assertTrue("Capability should match requirement", ResourceHelper.matches(req, cap));
	}
}
