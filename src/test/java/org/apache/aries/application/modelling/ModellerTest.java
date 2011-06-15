package org.apache.aries.application.modelling;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.aries.application.modelling.ExportedPackage;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.impl.ModelledResourceManagerImpl;
import org.apache.aries.application.modelling.impl.ModellingManagerImpl;
import org.apache.aries.application.modelling.impl.ParserProxyTest;
import org.apache.aries.application.modelling.standalone.OfflineModellingFactory;
import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.util.filesystem.FileSystem;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class ModellerTest {
    
    @Parameters
    public static List<Object[]> getDifferentModelledResourceManagers() {
        ModelledResourceManagerImpl manager = new ModelledResourceManagerImpl();
        manager.setModellingManager(new ModellingManagerImpl());
        manager.setParserProxy(ParserProxyTest.getMockParserServiceProxy());
        
        return Arrays.asList(new Object[][] {
                {OfflineModellingFactory.getModelledResourceManager()},
                {manager}
        });
    }
    
    @AfterClass
    public static void cleanup() {
        BundleContextMock.clear();
    }
    
	private final ModelledResourceManager sut;
	
	public ModellerTest(ModelledResourceManager sut) {
	    this.sut = sut;
	}
	
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
