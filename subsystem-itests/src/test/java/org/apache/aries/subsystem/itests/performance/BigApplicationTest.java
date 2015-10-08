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
package org.apache.aries.subsystem.itests.performance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.aries.subsystem.core.archive.PreferredProviderHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class BigApplicationTest extends AbstractPerformanceTest {
    public static void main(String[] args) throws IOException {
        BigApplicationTest test = new BigApplicationTest();
        InputStream is = test.createApplication("application");
        FileOutputStream fos = new FileOutputStream("application.esa");
        copy(is, fos);
        is.close();
        fos.close();
    }
    
    @Test
    @org.junit.Ignore
    public void testBigApplication() throws Exception {
        runTrials(createCallables());
    }

    private InputStream createApplication(String symbolicName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addBundles(zos, "preferredbundle", "package", Constants.EXPORT_PACKAGE);
        addBundles(zos, "exportbundle", "package", Constants.EXPORT_PACKAGE);
        addBundles(zos, "importbundle", "package", Constants.IMPORT_PACKAGE);
        zos.putNextEntry(new ZipEntry("OSGI-INF/SUBSYSTEM.MF"));
        StringBuilder preferredProviders = new StringBuilder("preferredbundle0;type=osgi.bundle");
        for (int i = 1; i < BUNDLE_COUNT; i++) {
            preferredProviders.append(",preferredbundle").append(i).append(";type=osgi.bundle");
        }
        new SubsystemManifest.Builder()
                .symbolicName(symbolicName)
                .type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION)
                .header(new PreferredProviderHeader(preferredProviders.toString()))
                .build()
                .write(zos);
        zos.closeEntry();
        zos.close();
        return new ByteArrayInputStream(baos.toByteArray());
    }
    
    private Collection<Callable<Subsystem>> createCallables() {
        Callable<Subsystem> callable = new Callable<Subsystem>() {
            @Override
            public Subsystem call() throws Exception {
                Subsystem subsystem = getRootSubsystem().install("application", createApplication("application"));
                return subsystem;
            }
        };
        return Collections.singletonList(callable);
    }
}
