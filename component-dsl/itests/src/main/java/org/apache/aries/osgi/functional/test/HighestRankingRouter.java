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
package org.apache.aries.osgi.functional.test;

import org.apache.aries.osgi.functional.Event;
import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.SentEvent;
import org.osgi.framework.ServiceReference;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Consumer;

import static org.apache.aries.osgi.functional.OSGi.serviceReferences;

/**
 * @author Carlos Sierra Andrés
 */
public class HighestRankingRouter<T extends Comparable<? super T>>
    implements Consumer<OSGi.Router<T>> {

    private PriorityQueue<Event<T>> _instances;
    private SentEvent sent;

    public static <T> OSGi<ServiceReference<T>> highest(Class<T> clazz) {
        return serviceReferences(clazz).route(new HighestRankingRouter<>());
    }

    @Override
    public void accept(OSGi.Router<T> router) {
        router.onIncoming(sr -> {
            _instances.add(sr);

            if (_instances.peek() == sr) {
                if (sent != null) {
                    sent.terminate();
                }

                sent = router.signalAdd(sr);
            }

        });

        router.onLeaving(sr -> {
            Event<T> old = _instances.peek();

            _instances.remove(sr);

            Event<T> current = _instances.peek();

            if (current != old) {
                sent.terminate();

                if (current != null) {
                    sent = router.signalAdd(current);
                }
            }
        });

        router.onStart(
            () -> _instances = new PriorityQueue<>(
                Comparator.<Event<T>, T>comparing(Event::getContent).
                    reversed()));

        router.onClose(() -> {
            if (sent != null) {
                sent.terminate();
            }

            _instances.clear();
        });
    }
}
