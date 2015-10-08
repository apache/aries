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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.aries.subsystem.itests.SubsystemTest;
import org.easymock.internal.matchers.Null;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;

public abstract class AbstractPerformanceTest extends SubsystemTest {
    protected static final int ARRAY_SIZE_BYTES = 2048;
    protected static final int BUNDLE_COUNT = 25;
    protected static final int PACKAGE_COUNT = 10;
    protected static final int THREAD_COUNT = 1;
    protected static final int TRIAL_COUNT = 1;
    
    protected final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    
    protected void addBundles(ZipOutputStream zos, String symbolicNamePrefix, String packageNamePrefix, String importOrExport) throws IOException {
        for (int i = 0; i < BUNDLE_COUNT; i++) {
            String symbolicName = symbolicNamePrefix + i;
            zos.putNextEntry(new ZipEntry(symbolicName + ".jar"));
            InputStream is = createBundle(symbolicName, packageNamePrefix, importOrExport);
            copy(is, zos);
            is.close();
            zos.closeEntry();
        }
    }
    
    protected static double average(long[] values) {
        double sum = 0;
        for (long value : values) {
            sum += value;
        }
        return sum / values.length;
    }
    
    protected static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] bytes = new byte[ARRAY_SIZE_BYTES];
        int read;
        while ((read = is.read(bytes)) != -1) {
            os.write(bytes, 0, read);
        }
    }
    
    protected InputStream createBundle(String symbolicName, String packageNamePrefix, String importOrExport) {
        TinyBundle tinyBundle = TinyBundles.bundle();
        tinyBundle.set(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
        StringBuilder builder = new StringBuilder(packageNamePrefix + 0);
        for (int i = 1; i < PACKAGE_COUNT; i++) {
            builder.append(',');
            builder.append(packageNamePrefix + i);
        }
        tinyBundle.set(importOrExport, builder.toString());
        InputStream is = tinyBundle.build();
        return is;
    }
    
    protected static Collection<Callable<Null>> createUninstallSubsystemCallables(Collection<Future<Subsystem>> futures) {
        Collection<Callable<Null>> callables = new ArrayList<Callable<Null>>(futures.size());
        for (Future<Subsystem> future : futures) {
            try {
                final Subsystem subsystem = future.get();
                callables.add(new Callable<Null>() {
                    @Override
                    public Null call() throws Exception {
                        subsystem.uninstall();
                        return null;
                    }
                });
            }
            catch (Exception e) {}
        }
        return callables;
    }
    
    protected void runTrials(Collection<Callable<Subsystem>> callables) throws InterruptedException {
        long[] times = new long[TRIAL_COUNT];
        for (int i = 0; i < TRIAL_COUNT; i++) {
            long start = System.currentTimeMillis();
            Collection<Future<Subsystem>> futures = executor.invokeAll(callables);
            long end = System.currentTimeMillis();
            times[i] = end - start;
            System.out.println("Trial " + i + " took " + times[i] + " ms");
            uninstallSubsystems(futures);
        }
        System.out.println("Average time across " + TRIAL_COUNT + " trials: " + average(times) + " ms");
        executor.shutdownNow();
    }
    
    protected void uninstallSubsystems(Collection<Future<Subsystem>> futures) throws InterruptedException {
        Collection<Callable<Null>> callables = createUninstallSubsystemCallables(futures);
        executor.invokeAll(callables);
    }
}
