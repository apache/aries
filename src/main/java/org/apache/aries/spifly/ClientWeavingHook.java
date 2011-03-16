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
package org.apache.aries.spifly;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.aries.spifly.api.SpiFlyConstants;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

public class ClientWeavingHook implements WeavingHook {
    private final String addedImport;
    private final Map<Bundle, WeavingData []> bundleWeavingData = new ConcurrentHashMap<Bundle, WeavingData[]>();
    
    ClientWeavingHook(BundleContext context) {
        Bundle b = context.getBundle();
        String bver = b.getVersion().toString();
        String bsn = b.getSymbolicName();
        
        addedImport = Util.class.getPackage().getName() + 
            ";bundle-symbolic-name=" + bsn + 
            ";bundle-version=" + bver;
        
        // TODO move to activator ? and bt.close()!
        BundleTracker<Object> bt = new BundleTracker<Object>(context, Bundle.INSTALLED, null) {
            @Override
            public Object addingBundle(Bundle bundle, BundleEvent event) {
                String consumerHeader = bundle.getHeaders().get(SpiFlyConstants.SPI_CONSUMER_HEADER);
                if (consumerHeader != null) {
                    WeavingData[] wd = ConsumerHeaderProcessor.processHeader(consumerHeader);
                    bundleWeavingData.put(bundle, wd);
                    
                    for (WeavingData w : wd) {
                        Activator.activator.registerConsumerBundle(bundle, w.getArgRestrictions(), w.getAllowedBundles());
                    }
                }                    
                
                return super.addingBundle(bundle, event);
            }

            @Override
            public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
                removedBundle(bundle, event, object);
                addingBundle(bundle, event);
            }

            @Override
            public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
                bundleWeavingData.remove(bundle);
            }
        };
        bt.open();
    }
    
	@Override
	public void weave(WovenClass wovenClass) {
	    Bundle consumerBundle = wovenClass.getBundleWiring().getBundle();
        WeavingData[] wd = bundleWeavingData.get(consumerBundle);
        if (wd != null) {
	        Activator.activator.log(LogService.LOG_DEBUG, "Weaving class " + wovenClass.getClassName());            
            
//            WeavingData[] wd = ConsumerHeaderProcessor.processHeader(consumerBundle, consumerHeader);
	        
	        ClassReader cr = new ClassReader(wovenClass.getBytes());
	        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
	        TCCLSetterVisitor tsv = new TCCLSetterVisitor(cw, wovenClass.getClassName(), wd);
	        cr.accept(tsv, 0);	        
	        wovenClass.setBytes(cw.toByteArray());
	        if (tsv.additionalImportRequired())
	            wovenClass.getDynamicImports().add(addedImport);
	    }			
	}
}
