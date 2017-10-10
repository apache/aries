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

import org.apache.aries.osgi.functional.Event;
import org.apache.aries.osgi.functional.SentEvent;

import java.util.function.Consumer;

public class RouteOsgiImpl<T> extends OSGiImpl<T> {

    public RouteOsgiImpl(
        OSGiImpl<T> previous, Consumer<Router<T>> routerConsumer) {

        super((bundleContext, op) -> {
            final RouterImpl<T> router =
                new RouterImpl<>(op);

            routerConsumer.accept(router);

            OSGiResultImpl osgiResult = previous._operation.run(
                bundleContext,
                t -> {
                    router._adding.accept(t);

                    t.onTermination(() -> router._leaving.accept(t));
                });

            return new OSGiResultImpl(
                () -> {
                    router._start.run();
                    osgiResult.start.run();
                },
                () -> {
                    router._close.run();
                    osgiResult.close.run();
                });
        });
    }

    static class RouterImpl<T> implements Router<T> {

        RouterImpl(Consumer<Tuple<T>> op) {
            this.op = op;
        }

        @Override
        public void onIncoming(Consumer<Event<T>> adding) {
            _adding = adding;
        }

        @Override
        public void onLeaving(Consumer<Event<T>> removing) {
            _leaving = removing;
        }

        @Override
        public void onClose(Runnable close) {
            _close = close;
        }

        @Override
        public void onStart(Runnable start) {
            _start = start;
        }

        @Override
        public SentEvent<T> signalAdd(Event<T> event) {
            Tuple<T> tuple = (Tuple<T>) event;

            Tuple<T> copy = tuple.copy();

            op.accept(copy);

            return copy;
        }

        Consumer<Event<T>> _adding = (ign) -> {};
        Consumer<Event<T>> _leaving = (ign) -> {};

        private Runnable _close = NOOP;
        private final Consumer<Tuple<T>> op;
        private Runnable _start = NOOP;

    }
}
