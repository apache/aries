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
package testweavinghook;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;

public class MyWeavingHook implements WeavingHook {

	@Override
	public void weave(WovenClass wovenClass) {
	    System.out.println("*** WovenClass: " + wovenClass.getClassName());
		if (wovenClass.getClassName().startsWith("mytestbundle")) {
			BundleWiring bw = wovenClass.getBundleWiring();
			String fileName = wovenClass.getClassName().replace('.', '/') + ".class";
			URL res = bw.getBundle().getResource("/altclasses/" + fileName);
			if (res != null) {
				System.out.println("*** Found an alternative class: " + res);
				try {
					wovenClass.setBytes(Streams.suck(res.openStream()));
					List<String> imports = wovenClass.getDynamicImports();
					imports.add("org.apache.aries.spifly.util");
					imports.add("org.osgi.util.tracker");
					imports.add("org.osgi.framework.wiring");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}				
		}			
	}
}
