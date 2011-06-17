package org.apache.aries.application.modelling;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Manifest;

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
import org.apache.aries.util.filesystem.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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

    @BeforeClass
    public static void setup() throws Exception {
        URL pathToTestBundle = ModellerTest.class.getClassLoader().getResource("test.bundle");
        File testBundleDir = new File(pathToTestBundle.toURI());
        File outputArchive = new File(testBundleDir.getParentFile(), "test.bundle.jar");

        FileInputStream fis = new FileInputStream(new File(testBundleDir, "META-INF/MANIFEST.MF"));
        Manifest manifest = new Manifest(fis);
        fis.close();

        IOUtils.jarUp(testBundleDir, outputArchive, manifest);
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
    public void testParsingByInputStreamProvider() throws Exception {
        final URL pathToTestBundle = getClass().getClassLoader().getResource("test.bundle.jar");

        ModelledResource resource = sut.getModelledResource("file:///test.bundle.uri", 
                new ModelledResourceManager.InputStreamProvider() {                    
                    public InputStream open() throws IOException {
                        return pathToTestBundle.openStream();
                    }
                }
        );
        
        checkTestBundleResource(resource);
    }

    @Test
    public void testParsingOfBundle() throws Exception {
        URL pathToTestBundle = getClass().getClassLoader().getResource("test.bundle");

        ModelledResource resource = sut.getModelledResource(
                "file:///test.bundle.uri", 
                FileSystem.getFSRoot(new File(pathToTestBundle.toURI())));

        checkTestBundleResource(resource);
    }

    private void checkTestBundleResource(ModelledResource resource) {
        assertNotNull(resource);

        assertEquals("file:///test.bundle.uri", resource.getLocation());
        
        // sanity check that we have parsed the manifest and package imports / exports

        assertEquals("test.bundle", resource.getSymbolicName());
        assertEquals("1.0.0", resource.getVersion());
        assertEquals(1, resource.getExportedPackages().size());
        assertEquals(3, resource.getImportedPackages().size());

        boolean foundFirstPackage = false;

        for (ImportedPackage pack : resource.getImportedPackages()) {
            if ("javax.jms".equals(pack.getPackageName()) && "1.1.0".equals(pack.getVersionRange()))
                foundFirstPackage = true;
        }

        assertTrue(foundFirstPackage);

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
