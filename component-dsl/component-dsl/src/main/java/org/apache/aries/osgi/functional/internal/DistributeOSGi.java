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

package org.apache.aries.osgi.functional.internal;

import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Carlos Sierra Andrés
 */
public class DistributeOSGi<T> extends OSGiImpl<T> {

    @SafeVarargs
    public DistributeOSGi(OSGi<T>... programs) {
        super(bundleContext -> {
            Pipe<Tuple<T>, Tuple<T>> added = Pipe.create();

            Consumer<Tuple<T>> addedSource = added.getSource();

            List<OSGiResult<T>> results = new ArrayList<>();

            Pipe<Tuple<T>, Tuple<T>> removed = Pipe.create();

            Consumer<Tuple<T>> removedSource = removed.getSource();

            return new OSGiResultImpl<>(
                added, removed,
                () -> {
                    results.addAll(
                        Arrays.stream(programs).
                            map(o -> {
                                OSGiResultImpl<T> osGiResult =
                                    ((OSGiImpl<T>) o)._operation.run(
                                        bundleContext);

                                osGiResult.added.map(t -> {addedSource.accept(t); return null;});
                                osGiResult.removed.map(t -> {removedSource.accept(t); return null;});

                                osGiResult.start.run();

                                return osGiResult;
                            }).
                            collect(Collectors.toList()));
                },
                () -> {
                    for (OSGiResult<?> result : results) {
                        try {
                            result.close();
                        }
                        catch (Exception ignored) {
                        }
                    }

                }
            );
        });
    }
}
