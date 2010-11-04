/**
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
package mytestbundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.aries.spifly.mysvc.SPIProvider;
import org.apache.aries.spifly.util.MultiDelegationClassloader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;

public class Activator implements BundleActivator {
	public void start(BundleContext context) throws Exception {
		callSPI_Around();
	}

	private void callSPI_Around() throws Exception {
		ClassLoader cl = Activator.class.getClassLoader();
		if (!(cl instanceof BundleReference)) {
			// can't do anything
			System.out.println("cl is not an instance of BundleReference");
			callSPI();
			return;
		}
			
		BundleReference bref = (BundleReference) cl;
		BundleContext ctx = bref.getBundle().getBundleContext();
		
//		ServiceReference<?>[] refs0 = ctx.getAllServiceReferences("org.osgi.service.packageadmin.PackageAdmin", null);
//		System.out.println("References to org.osgi.service.packageadmin.PackageAdmin: " + Arrays.toString(refs0));
//		ServiceReference<?>[] refs1 = ctx.getAllServiceReferences("org.apache.aries.spifly.api.SPIClassloaderAdviceService", null);
//		System.out.println("References to org.apache.aries.spifly.api.SPIClassloaderAdviceService: " + Arrays.toString(refs1));
//		ServiceReference<?>[] refs2 = ctx.getAllServiceReferences("org.apache.aries.spifly.mysvc.SPIProvider", null);
//		System.out.println("References to org.apache.aries.spifly.mysvc.SPIProvider: " + Arrays.toString(refs2));
//		ServiceReference<?>[] refs3 = ctx.getAllServiceReferences(null, null);
//        System.out.println("All Service References: " + Arrays.toString(refs3));		
//        ServiceReference<?>[] refs4 = ctx.getAllServiceReferences("org.apache.aries.spifly.api.SPIClassloaderAdviceService", null);
//        System.out.println("References to org.apache.aries.spifly.api.SPIClassloaderAdviceService: " + Arrays.toString(refs4));
		
		String className = SPIProvider.class.getName(); // obtain through aspect
		ServiceReference<?>[] refs = ctx.getAllServiceReferences(className, null);
		System.out.println("References to " + className + ": " + Arrays.toString(refs));
		List<ClassLoader> loaders = new ArrayList<ClassLoader>();
		if (refs != null) {
			for (ServiceReference<?> ref : refs) {
				Bundle b = ref.getBundle();
				BundleWiring bw = b.adapt(BundleWiring.class);
				loaders.add(bw.getClassLoader());
			}
		}
	
		if (loaders.size() == 0) {
			callSPI();
			return;
		}
		
		ClassLoader targetLoader = loaders.size() > 1 ? new MultiDelegationClassloader(loaders.toArray(new ClassLoader[0])) : loaders.get(0);
		ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
		try {
            System.out.println("Setting thread context classloader to " + targetLoader);
            Thread.currentThread().setContextClassLoader(targetLoader);
            callSPI();
            return;            
        } finally {
            Thread.currentThread().setContextClassLoader(prevCl);                       			
		}
	}
	
	private void callSPI() {
		System.out.println("(Modified) *** Loading the SPI...");
		ServiceLoader<SPIProvider> ldr = ServiceLoader.load(SPIProvider.class);
        for (SPIProvider spiObject : ldr) {
        	System.out.println("*** Invoking the SPI...");
            spiObject.doit(); // invoke the SPI object
        }
	}
	
	public void stop(BundleContext context) throws Exception {
	}
}
