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

import org.apache.aries.osgi.functional.Publisher;
import org.apache.aries.osgi.functional.Transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.apache.aries.osgi.functional.OSGi.NOOP;

/**
 * @author Carlos Sierra Andrés
 */
public class AccumulateTransformer<T> implements Transformer<T, List<T>> {

    @Override
    public Publisher<T> transform(Publisher<List<T>> op) {
        ConcurrentDoublyLinkedList<T> list =
            new ConcurrentDoublyLinkedList<>();

        AtomicReference<Runnable> terminator = new AtomicReference<>(NOOP);

        return t -> {
            ConcurrentDoublyLinkedList.Node node = list.addLast(t);

            publish(op, list, terminator);

            return () -> {
                node.remove();

                publish(op, list, terminator);
            };
        };
    }

    private static <T> void publish(
        Function<List<T>, Runnable> op, ConcurrentDoublyLinkedList<T> list,
        AtomicReference<Runnable> terminator) {

        Runnable runnable = terminator.get();

        runnable.run();

        terminator.set(op.apply(new ArrayList<>(list)));
    }

}
