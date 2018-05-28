/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.component.dsl.internal;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.aries.component.dsl.Publisher;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Carlos Sierra Andrés
 */
public class CoalesceOSGiImpl<T> extends OSGiImpl<T> {

    @SafeVarargs
    public CoalesceOSGiImpl(OSGi<T>... programs) {
        super((bundleContext, op) -> {
            AtomicBoolean initialized = new AtomicBoolean();
            AtomicInteger[] atomicIntegers = new AtomicInteger[programs.length];
            OSGiResult[] results = new OSGiResult[programs.length];
            AtomicInteger index = new AtomicInteger();
            Publisher<T>[] publishers = new Publisher[programs.length];

            for (int i = 0; i < atomicIntegers.length; i++) {
                atomicIntegers[i] = new AtomicInteger();
            }

            for (int i = 0; i < atomicIntegers.length; i++) {
                AtomicInteger atomicInteger = atomicIntegers[i];

                final int pos = i;

                publishers[i] = t -> {
                    OSGiResult result;

                    synchronized (initialized) {
                        atomicInteger.incrementAndGet();

                        if (initialized.get()) {
                            int indexInt = index.getAndSet(pos);

                            if (pos < indexInt) {
                                for (int j = pos + 1; j <= indexInt; j++) {
                                    results[j].close();
                                }

                            }
                        }

                        result = op.publish(t);
                    }

                    return () -> {
                        synchronized (initialized) {
                            result.close();

                            UpdateSupport.defer(() -> {
                                int current = atomicInteger.decrementAndGet();

                                if (!initialized.get()) {
                                    return;
                                }

                                if (pos <= index.get() && current == 0) {
                                    for (int j = pos + 1; j < results.length; j++) {
                                        results[j] = programs[j].run(
                                            bundleContext, publishers[j]);

                                        index.set(j);

                                        if (atomicIntegers[j].get() > 0) {
                                            break;
                                        }
                                    }
                                }
                            });
                        }
                    };
                };
            }

            synchronized (initialized) {
                for (int i = 0; i < publishers.length; i++) {

                    results[i] = programs[i].run(bundleContext, publishers[i]);

                    index.set(i);

                    if (atomicIntegers[i].get() > 0) {
                        initialized.set(true);

                        break;
                    }
                }

                initialized.set(true);
            }

            return new OSGiResultImpl(
                () -> {
                    synchronized (initialized) {
                        initialized.set(false);

                        for (int i = 0; i <= index.get(); i++) {
                            results[i].close();
                        }
                    }
                }
            );
        });
    }
}
