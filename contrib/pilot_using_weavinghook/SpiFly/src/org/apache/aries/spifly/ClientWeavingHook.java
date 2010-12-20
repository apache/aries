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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.aries.spifly.HeaderParser.PathElement;
import org.apache.aries.spifly.api.SpiFlyConstants;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.service.log.LogService;

public class ClientWeavingHook implements WeavingHook {
    private final BundleContext bundleContext;
    private final String addedImport;
    
    ClientWeavingHook(BundleContext context) {
        bundleContext = context;
        
        Bundle b = context.getBundle();
        String bver = b.getVersion().toString();
        String bsn = b.getSymbolicName();
        
        addedImport = Util.class.getPackage().getName() + 
            ";bundle-symbolic-name=" + bsn + 
            ";bundle-version=" + bver;
    }
    
	@Override
	public void weave(WovenClass wovenClass) {
	    Bundle consumerBundle = wovenClass.getBundleWiring().getBundle();
        String consumerHeader = consumerBundle.getHeaders().get(SpiFlyConstants.SPI_CONSUMER_HEADER);
        if (consumerHeader != null) {
	        Activator.activator.log(LogService.LOG_DEBUG, "Weaving class " + wovenClass.getClassName());            
            
            WeavingData wd = parseHeader(consumerBundle, consumerHeader);
	        
	        ClassReader cr = new ClassReader(wovenClass.getBytes());
	        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
	        TCCLSetterVisitor tsv = new TCCLSetterVisitor(cw, wovenClass.getClassName(), wd);
	        cr.accept(tsv, 0);
	        wovenClass.setBytes(cw.toByteArray());
	        wovenClass.getDynamicImports().add(addedImport);
	    }			
	}

    private WeavingData parseHeader(Bundle consumerBundle, String consumerHeader) {
        List<Bundle> selectedBundles = new ArrayList<Bundle>();

        for (PathElement element : HeaderParser.parseHeader(consumerHeader)) {
            String name = element.getName().trim();
            String className;
            String methodName;
            int hashIdx = name.indexOf('#');
            if (hashIdx > 0) {
                className = name.substring(0, hashIdx);
                int braceIdx = name.substring(hashIdx).indexOf('(');
                if (braceIdx > 0) {
                    methodName = name.substring(hashIdx + 1, hashIdx + braceIdx);
                } else {
                    methodName = name.substring(hashIdx + 1);                    
                }
            } else {
                if ("true".equalsIgnoreCase(name)) {
                    className = ServiceLoader.class.getName();
                    methodName = "load";
                } else {
                    className = name;
                    methodName = null;
                }
            }            
                
            String bsn = element.getAttribute("bundle");
            if (bsn != null) {
                for (Bundle b : bundleContext.getBundles()) {
                    if (b.getSymbolicName().equals(bsn)) {
                        selectedBundles.add(b);
                        break;                        
                    }
                }
            }
            
            Activator.activator.log(LogService.LOG_INFO, "Weaving " + className + "#" + methodName + " from bundle " + 
                consumerBundle.getSymbolicName() + " to " + (selectedBundles.size() == 0 ? " any provider" : selectedBundles));
            
            if (selectedBundles.size() > 0)
                Activator.activator.registerConsumerBundle(consumerBundle, selectedBundles);
           
            // TODO support more than one definition
            return new WeavingData(className, methodName, 1);
        }
        return null;
    }
}
