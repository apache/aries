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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;

public class MyWeavingHook implements WeavingHook {
    private final String addedImport;
    
    MyWeavingHook(BundleContext context) {
        Bundle b = context.getBundle();
        String bver = b.getVersion().toString();
        String bsn = b.getSymbolicName();
        
        addedImport = Util.class.getPackage().getName() + 
            ";bundle-symbolic-name=" + bsn + 
            ";bundle-version=" + bver;
    }
    
	@Override
	public void weave(WovenClass wovenClass) {
	    // TODO base this on SPI-Consumer header
	    if (wovenClass.getBundleWiring().getBundle().getSymbolicName().equals("MyTestBundle"))
	    {
	        System.out.println("*** WovenClass: " + wovenClass.getClassName());
	        
	        ClassReader cr = new ClassReader(wovenClass.getBytes());
	        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
	        TCCLSetterVisitor tsv = new TCCLSetterVisitor(cw, wovenClass.getClassName());
	        cr.accept(tsv, 0);
	        wovenClass.setBytes(cw.toByteArray());
	        wovenClass.getDynamicImports().add(addedImport);
	    }			
	}
}
