/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.subsystem.itests.defect;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;

import org.apache.aries.subsystem.core.archive.AriesProvisionDependenciesDirective;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.SubsystemArchiveBuilder;
import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class Aries1523Test extends SubsystemTest {
	private RepositoryAdmin repositoryAdmin;
	private URL url;
	
	@Override
    public void setUp() throws Exception {
        super.setUp();
        BundleContext context = context();
        ServiceReference<RepositoryAdmin> ref = context.getServiceReference(RepositoryAdmin.class);
        assertNotNull("The RepositoryAdmin service does not exist", ref);
        try {
        	repositoryAdmin = (RepositoryAdmin)context.getService(ref);
        	DataModelHelper helper = repositoryAdmin.getHelper();
        	url = createRepositoryXml(helper);
        	Repository repository = repositoryAdmin.addRepository(url);
        	Resource resource = repository.getResources()[0];
        	System.out.println(resource.getURI());
        }
        finally {
        	context.ungetService(ref);
        }
    }
	
	@Override
	public void tearDown() throws Exception {
		repositoryAdmin.removeRepository(url.toString());
		super.tearDown();
	}
	
	@Test
	public void testApacheAriesProvisionDependenciesInstall() throws Exception {
		test(AriesProvisionDependenciesDirective.INSTALL);
	}
	
	@Test
	public void testApacheAriesProvisionDependenciesResolve() throws Exception {
		test(AriesProvisionDependenciesDirective.RESOLVE);
	}
	
	private void test(AriesProvisionDependenciesDirective provisionDependencies) throws Exception {
		boolean flag = AriesProvisionDependenciesDirective.INSTALL.equals(provisionDependencies);
		Subsystem root = getRootSubsystem();
		try {
			Subsystem subsystem = installSubsystem(
					root,
					"subsystem", 
					new SubsystemArchiveBuilder()
							.symbolicName("subsystem")
							.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' 
										+ provisionDependencies.toString())
							.content("org.apache.aries.subsystem.itests.aries1523host,org.apache.aries.subsystem.itests.aries1523fragment")
							.bundle(
									"aries1523fragment", 
									getClass().getClassLoader().getResourceAsStream("aries1523/aries1523fragment.jar"))
							.build(),
					flag
			);
			try {
				startSubsystem(subsystem, flag);
				stopSubsystem(subsystem);
			}
			catch (SubsystemException e) {
				e.printStackTrace();
				fail("Subsystem should have started");
			}
			finally {
				uninstallSubsystemSilently(subsystem);
			}
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			fail("Subsystem should have installed");
		}
	}
	
	private URL createRepositoryXml(DataModelHelper helper) throws Exception {
		File dir;
		String cwd = new File("").getAbsolutePath();
		if (cwd.endsWith(File.separator + "target")) {
			dir = new File("test-classes/aries1523");
		}
		else {
			dir = new File("target/test-classes/aries1523");
		}
		File jar = new File(dir, "aries1523host.jar");
		assertTrue("The host jar does not exist: " + jar.getAbsolutePath(), jar.exists());
		Resource resource = helper.createResource(jar.toURI().toURL());
		Repository repository = helper.repository(new Resource[] {resource});
		File file = new File(dir, "repository.xml");
		FileWriter fw = new FileWriter(file);
		try {
			helper.writeRepository(repository, fw);
			return file.toURI().toURL();
		}
		finally {
			fw.close();
		}
	}
}
