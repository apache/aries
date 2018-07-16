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
package org.apache.aries.spifly.dynamic;

import java.util.Set;

import org.apache.aries.spifly.Util;
import org.apache.aries.spifly.WeavingData;
import org.apache.aries.spifly.weaver.TCCLSetterVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.service.log.LogService;

public class ClientWeavingHook implements WeavingHook {
    private final String addedImport;
    private final DynamicWeavingActivator activator;

    ClientWeavingHook(BundleContext context, DynamicWeavingActivator dwActivator) {
        activator = dwActivator;

        addedImport = Util.class.getPackage().getName();
    }

    @Override
    public void weave(WovenClass wovenClass) {
        Bundle consumerBundle = wovenClass.getBundleWiring().getBundle();
        Set<WeavingData> wd = activator.getWeavingData(consumerBundle);
        if (wd != null) {
            activator.log(LogService.LOG_DEBUG, "Weaving class " + wovenClass.getClassName());

            ClassReader cr = new ClassReader(wovenClass.getBytes());
            ClassWriter cw = new OSGiFriendlyClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES,
                wovenClass.getBundleWiring().getClassLoader());
            TCCLSetterVisitor tsv = new TCCLSetterVisitor(cw, wovenClass.getClassName(), wd);
            cr.accept(tsv, ClassReader.SKIP_FRAMES);
            if (tsv.isWoven()) {
                wovenClass.setBytes(cw.toByteArray());
                if (tsv.additionalImportRequired())
                    wovenClass.getDynamicImports().add(addedImport);
            }
        }
    }
}
