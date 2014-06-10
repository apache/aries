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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.itests.util.GenericMetadataWrapper;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class ServiceDependencyTest extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Subsystem-Content: bundle.a.jar
	 */
	private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Subsystem-SymbolicName: application.b.esa
	 * Subsystem-Content: bundle.b.jar
	 */
	private static final String APPLICATION_B = "application.b.esa";
	/*
	 * Subsystem-SymbolicName: application.b.esa
	 * Subsystem-Content: bundle.a.jar, bundle.b.jar
	 */
	private static final String APPLICATION_C = "application.c.esa";
	/*
	 * Subsystem-SymbolicName: application.d.esa
	 * Subsystem-Content: bundle.a.jar, composite.a.esa
	 */
	private static final String APPLICATION_D = "application.d.esa";
	/*
	 * Subsystem-SymbolicName: composite.a.esa
	 * Subsystem-Content: bundle.b.jar
	 */
	private static final String COMPOSITE_A = "composite.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Bundle-Blueprint: OSGI-INF/blueprint/*.xml
	 * 
	 * <blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0">
	 * 		<reference interface="bundle.b"/>
	 * 		<reference interface="bundle.b1" filter="(active=true)"/>
	 * 		<service interface="bundle.a" ref="bundle.a"/>
	 * </blueprint>
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Bundle-Blueprint: OSGI-INF/blueprint/*.xml
	 * 
	 * <blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0">
	 * 		<reference interface="bundle.a" availability="optional"/>
	 * 		<service ref="bundle.b">
	 * 			<interfaces>
	 * 				<value>bundle.b</value>
	 * 				<value>bundle.b1</value>
	 * 			</interfaces>
	 * 			<service-properties>
	 * 				<entry key="active">
	 * 					<value type="java.lang.Boolean">true</value>
	 * 				</entry>
	 * 				<entry key="mode" value="shared"/>
	 * 			</service-properties>
	 * 		</service>
	 * </blueprint>
	 */
	private static final String BUNDLE_B = "bundle.b.jar";
	
	private static void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A, BUNDLE_A);
	}
	
	private static void createApplicationB() throws IOException {
		createApplicationBManifest();
		createSubsystem(APPLICATION_B, BUNDLE_B);
	}
	
	private static void createApplicationC() throws IOException {
		createApplicationCManifest();
		createSubsystem(APPLICATION_C, BUNDLE_A, BUNDLE_B);
	}
	
	private static void createApplicationD() throws IOException {
		createApplicationDManifest();
		createSubsystem(APPLICATION_D, BUNDLE_A, COMPOSITE_A);
	}
	
	private static void createApplicationAManifest() throws IOException {
		createBasicApplicationManifest(APPLICATION_A);
	}
	
	private static void createApplicationBManifest() throws IOException {
		createBasicApplicationManifest(APPLICATION_B);
	}
	
	private static void createApplicationCManifest() throws IOException {
		createBasicApplicationManifest(APPLICATION_C);
	}
	
	private static void createApplicationDManifest() throws IOException {
		createBasicApplicationManifest(APPLICATION_D);
	}
	
	private static void createBasicApplicationManifest(String symbolicName) throws IOException {
		createBasicSubsystemManifest(symbolicName, null, null);
	}
	
	private static void createBasicSubsystemManifest(String symbolicName, Version version, String type) throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, symbolicName);
		if (version != null)
			attributes.put(SubsystemConstants.SUBSYSTEM_VERSION, version.toString());
		if (type != null)
			attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, type);
		createManifest(symbolicName + ".mf", attributes);
	}
	

	
	private static void createBundleA() throws IOException {
		createBlueprintBundle(
				BUNDLE_A, 
				new StringBuilder()
					.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
					.append("<blueprint ")
					.append("xmlns=\"http://www.osgi.org/xmlns/blueprint/v1.0.0\">")
					.append("<reference ")
					.append("interface=\"bundle.b\"")
					.append("/>")
					.append("<service ")
					.append("interface=\"bundle.a\" ")
					.append("ref=\"bundle.a\"")
					.append("/>")
					.append("</blueprint>")
					.toString());
	}
	
	private static void createBundleB() throws IOException {
		createBlueprintBundle(
				BUNDLE_B, 
				new StringBuilder()
					.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
					.append("<blueprint ")
					.append("xmlns=\"http://www.osgi.org/xmlns/blueprint/v1.0.0\">")
					.append("<reference ")
					.append("interface=\"bundle.a\" ")
					.append("availability=\"optional\"")
					.append("/>")
					.append("<service ref=\"bundle.b\">")
					.append("<interfaces>")
					.append("<value>bundle.b</value>")
					.append("<value>bundle.b1</value>")
					.append("</interfaces>")
					.append("<service-properties>")
					.append("<entry key=\"active\">")
					.append("<value type=\"java.lang.Boolean\">true</value>")
					.append("</entry>")
					.append("<entry key=\"mode\" value=\"shared\"/>")
					.append("</service-properties>")
					.append("</service>")
					.append("</blueprint>")
					.toString());
	}
	
	private static void createCompositeA() throws IOException {
		createCompositeAManifest();
		createSubsystem(COMPOSITE_A, BUNDLE_B);
	}
	
	private static void createCompositeAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(
				SubsystemConstants.SUBSYSTEM_EXPORTSERVICE, 
				"bundle.b;filter:=\"(&(active=true)(mode=shared))\"");
		createManifest(COMPOSITE_A + ".mf", attributes);
	}
	
	@Override
	public void createApplications() throws Exception {
		createBundleA();
		createBundleB();
		createApplicationA();
		createApplicationB();
		createApplicationC();
		createCompositeA();
		createApplicationD();
	}
	
	//@Test
	public void testImportServiceDependencySatisfiedByChild() throws Exception {
		try {
			Subsystem subsystem = installSubsystemFromFile(APPLICATION_D);
			try {
				assertNull(
						"Generated application Subsystem-ImportService header when dependency satisfied by child",
						subsystem.getSubsystemHeaders(null).get(SubsystemConstants.SUBSYSTEM_IMPORTSERVICE));
				assertSubsystemExportServiceHeader(
						subsystem.getChildren().iterator().next(), 
						"bundle.b;filter:=\"(&(active=true)(mode=shared))\"");
			}
			finally {
				uninstallSubsystemSilently(subsystem);
			}
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			fail("Installation must succeed if missing service dependency is satisfied");
		}
	}
	
	@Test
	public void testImportServiceDependencySatisfiedByContent() throws Exception {
		try {
			Subsystem subsystem = installSubsystemFromFile(APPLICATION_C);
			try {
				assertNull(
						"Generated application Subsystem-ImportService header when dependency satisfied by content",
						subsystem.getSubsystemHeaders(null).get(SubsystemConstants.SUBSYSTEM_IMPORTSERVICE));
			}
			finally {
				uninstallSubsystemSilently(subsystem);
			}
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			fail("Installation must succeed if service dependency is satisfied");
		}
	}
	
	@Test
	public void testImportServiceDependencySatisfiedByParent() throws Exception {
		try {
			Subsystem parent = installSubsystemFromFile(APPLICATION_B);
			try {
				Subsystem child = installSubsystemFromFile(parent, APPLICATION_A);
				try {
					assertSubsystemImportServiceHeader(child, "osgi.service;filter:=\"(objectClass=bundle.b)\";resolution:=mandatory;cardinality:=single");
				}
				finally {
					uninstallSubsystemSilently(child);
				}
			}
			catch (SubsystemException e) {
				e.printStackTrace();
				fail("Installation must succeed if service dependency is satisfied");
			}
			finally {
				uninstallSubsystemSilently(parent);
			}
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			fail("Installation must succeed if missing service dependency is optional");
		}
	}
	
	@Test
	public void testMissingImportServiceDependencyMandatory() throws Exception {
		try {
			Subsystem subsystem = installSubsystemFromFile(APPLICATION_A);
			uninstallSubsystemSilently(subsystem);
			fail("Installation must fail due to missing service dependency");
		}
		catch (SubsystemException e) {
			// Okay.
		}
	}
	
	@Test
	public void testMissingImportServiceDependencyOptional() throws Exception {
		try {
			Subsystem subsystem = installSubsystemFromFile(APPLICATION_B);
			try {
				assertSubsystemImportServiceHeader(subsystem, "osgi.service;filter:=\"(objectClass=bundle.a)\";resolution:=optional;cardinality:=single");
			}
			finally {
				uninstallSubsystemSilently(subsystem);
			}
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			fail("Installation must succeed if missing service dependency is optional");
		}
	}
	
	private void assertSubsystemExportServiceHeader(Subsystem subsystem, String value) throws InvalidSyntaxException {
		String header = assertHeaderExists(subsystem, SubsystemConstants.SUBSYSTEM_EXPORTSERVICE);
		List<GenericMetadata> actual = ManifestHeaderProcessor.parseRequirementString(header);
		List<GenericMetadata> expected = ManifestHeaderProcessor.parseRequirementString(value);
		Assert.assertEquals("Wrong number of clauses", expected.size(), actual.size());
		for (int i = 0; i < expected.size(); i++)
			assertEquals("Wrong clause", new GenericMetadataWrapper(expected.get(i)), new GenericMetadataWrapper(actual.get(i)));
	}
	
	private void assertSubsystemImportServiceHeader(Subsystem subsystem, String value) throws InvalidSyntaxException {
		String header = assertHeaderExists(subsystem, SubsystemConstants.SUBSYSTEM_IMPORTSERVICE);
		List<GenericMetadata> actual = ManifestHeaderProcessor.parseRequirementString(header);
		List<GenericMetadata> expected = ManifestHeaderProcessor.parseRequirementString(value);
		Assert.assertEquals("Wrong number of clauses", expected.size(), actual.size());
		for (int i = 0; i < expected.size(); i++)
			assertEquals("Wrong clause", new GenericMetadataWrapper(expected.get(i)), new GenericMetadataWrapper(actual.get(i)));
	}
}
