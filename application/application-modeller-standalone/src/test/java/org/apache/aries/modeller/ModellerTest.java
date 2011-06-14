package org.apache.aries.modeller;

import java.io.File;
import java.net.URL;
import org.apache.aries.application.modelling.ExportedPackage;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.standalone.OfflineModellingFactory;
import org.apache.aries.util.filesystem.FileSystem;
import org.junit.Test;

import static org.junit.Assert.*;

public class ModellerTest {
	ModelledResourceManager sut = OfflineModellingFactory.getModelledResourceManager();
	
	@Test
	public void testParsingOfBundle() throws Exception {
		URL pathToTestBundle = getClass().getClassLoader().getResource("test.bundle");
		
		ModelledResource resource = sut.getModelledResource(
				"file:///some.uri", 
				FileSystem.getFSRoot(new File(pathToTestBundle.toURI())));
		
		assertNotNull(resource);
		
		// sanity check that we have parsed the manifest and package imports / exports
		
		assertEquals("test.bundle", resource.getSymbolicName());
		assertEquals("1.0.0", resource.getVersion());
		assertEquals(1, resource.getExportedPackages().size());
		assertEquals(3, resource.getImportedPackages().size());
		
		ImportedPackage pack = resource.getImportedPackages().iterator().next();
		assertEquals("javax.jms", pack.getPackageName());
		assertEquals("1.1.0", pack.getVersionRange());
		
		ExportedPackage epack = resource.getExportedPackages().iterator().next();
		assertEquals("wibble", epack.getPackageName());
		assertEquals("1.0.0", epack.getVersion());
		
		assertEquals("true", epack.getAttributes().get("directive:"));
		
		
		// sanity check that we have parsed the services
		
		assertEquals(3, resource.getExportedServices().size());
		assertEquals(1, resource.getImportedServices().size());
		
		ImportedService service = resource.getImportedServices().iterator().next();
		assertEquals("foo.bar.MyInjectedService", service.getInterface());
		assertTrue(service.isOptional());
		assertFalse(service.isList());
		assertEquals("anOptionalReference", service.getId());
	}
}
