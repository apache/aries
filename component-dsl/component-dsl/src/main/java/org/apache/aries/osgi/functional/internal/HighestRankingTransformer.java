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

import org.apache.aries.osgi.functional.Transformer;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.apache.aries.osgi.functional.OSGi.NOOP;

/**
 * @author Carlos Sierra Andrés
 */
public class HighestRankingTransformer<T> implements Transformer<T, T> {

    public HighestRankingTransformer(Comparator<? super T> comparator) {
        _comparator = comparator;
    }

    @Override
    public Function<T, Runnable> apply(Function<T, Runnable> publisher) {
        PriorityQueue<T> set = new PriorityQueue<>(_comparator.reversed());
        AtomicReference<Runnable> terminator = new AtomicReference<>(NOOP);

        return t -> {
            synchronized (set) {
                set.add(t);

                if (set.peek() == t) {
                    Runnable old = terminator.get();

                    old.run();

                    terminator.set(publisher.apply(t));
                }
            }

            return () -> {
                synchronized (set) {
                    T old = set.peek();

                    set.remove(t);

                    T current = set.peek();

                    if (current != old) {
                        terminator.getAndSet(NOOP).run();

                        if (current != null) {
                            terminator.set(publisher.apply(current));
                        }
                    }
                }
            };
        };
    }

    private Comparator<? super T> _comparator;
}
