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

import java.util.function.Consumer;

public class RouteOsgiImpl<T> extends OSGiImpl<T> {

    public RouteOsgiImpl(
        OSGiImpl<T> previous, Consumer<Router<T>> routerConsumer) {

        super(((bundleContext) -> {

            Pipe<Tuple<T>, Tuple<T>> outgoingAddingPipe = Pipe.create();

            Consumer<Tuple<T>> outgoingAddingSource =
                outgoingAddingPipe.getSource();

            final RouterImpl<T> router =
                new RouterImpl<>(outgoingAddingSource);

            routerConsumer.accept(router);

            OSGiResultImpl<T> osgiResult = previous._operation.run(
                bundleContext);

            osgiResult.added.map(
                t -> {
                    Tuple<T> copy = Tuple.create(t.t);

                    t.onTermination(() -> {
                        router._leaving.accept(copy);

                        copy.terminate();
                    });

                    router._adding.accept(copy);

                    return null;
                });

            return new OSGiResultImpl<>(
                outgoingAddingPipe,
                () -> {
                    router._start.run();
                    osgiResult.start.run();
                },
                () -> {
                    router._close.run();
                    osgiResult.close.run();
                });
        }));
    }

    static class RouterImpl<T> implements Router<T> {

        RouterImpl(Consumer<Tuple<T>> signalAdding) {
            _signalAdding = signalAdding;
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
        public void signalAdd(Event<T> event) {
            _signalAdding.accept((Tuple<T>) event);
        }

        @Override
        public void signalLeave(Event<T> event) {
            ((Tuple<T>)event).terminate();
        }

        Consumer<Event<T>> _adding = (ign) -> {};
        Consumer<Event<T>> _leaving = (ign) -> {};

        private Runnable _close = NOOP;
        private final Consumer<Tuple<T>> _signalAdding;
        private Runnable _start = NOOP;

    }
}
