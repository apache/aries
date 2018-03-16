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

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public class HighestRankingOSGi<T> extends OSGiImpl<T> {

    public HighestRankingOSGi(
        OSGi<T> previous, Comparator<? super T> comparator,
        Function<OSGi<T>, OSGi<T>> notHighest) {

        super((bundleContext, highestPipe) -> {
            Comparator<Tuple<T>> comparing = Comparator.comparing(
                Tuple::getT, comparator);
            PriorityQueue<Tuple<T>> set = new PriorityQueue<>(
                comparing.reversed());
            AtomicReference<Tuple<T>> sent = new AtomicReference<>();

            Pad<T, T> notHighestPad = new Pad<>(
                bundleContext, notHighest, highestPipe);

            OSGiResultImpl result = ((OSGiImpl<T>) previous)._operation.run(
                bundleContext,
                t -> {
                    Tuple<T> tuple = new Tuple<>(t);

                    synchronized (set) {
                        set.add(tuple);

                        if (set.peek() == tuple) {
                            Tuple<T> old = sent.get();

                            if (old != null) {
                                old._runnable.run();

                                old._runnable = notHighestPad.publish(old._t);
                            }

                            tuple._runnable = highestPipe.apply(t);

                            sent.set(tuple);
                        } else {
                            tuple._runnable = notHighestPad.publish(t);
                        }
                    }

                    return () -> {
                        synchronized (set) {
                            Tuple<T> old = set.peek();

                            set.remove(tuple);

                            Tuple<T> current = set.peek();

                            tuple._runnable.run();

                            if (current != old && current != null) {
                                current._runnable.run();
                                current._runnable = highestPipe.apply(
                                    current._t);
                                sent.set(current);
                            }
                            if (current == null) {
                                sent.set(null);
                            }
                        }
                    };
                });

            return new OSGiResultImpl(
                result::start,
                () -> {
                    result.close();

                    notHighestPad.close();
                });
        });
    }

    private static class Tuple<T> {

        Tuple(T t) {
            _t = t;
        }

        public T getT() {
            return _t;
        }
        T _t;
        Runnable _runnable;

    }

}
