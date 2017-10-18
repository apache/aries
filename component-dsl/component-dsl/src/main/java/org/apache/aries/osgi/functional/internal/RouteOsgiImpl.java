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
import java.util.function.Function;

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
                    EventImpl<T> event = new EventImpl<>(t);

                    router._adding.accept(event);

                    return () -> {
                        event.terminate();

                        router._leaving.accept(event);
                    };
                });

            return new OSGiResultImpl(
                () -> {
                    router._start.run();
                    osgiResult.start();
                },
                () -> {
                    router._close.run();
                    osgiResult.close();
                });
        });
    }

    static class RouterImpl<T> implements Router<T> {

        private final Function<T, Runnable> op;
        Consumer<Event<T>> _adding = (ign) -> {};
        Consumer<Event<T>> _leaving = (ign) -> {};
        private Runnable _close = NOOP;
        private Runnable _start = NOOP;

        RouterImpl(Function<T, Runnable> op) {
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
        public void onStart(Runnable start) {
            _start = start;
        }

        @Override
        public void onClose(Runnable close) {
            _close = close;
        }

        @Override
        public SentEvent<T> signalAdd(Event<T> event) {
            Runnable terminator = op.apply(event.getContent());

            ConcurrentDoublyLinkedList.Node node =
                ((EventImpl<T>) event).addTerminator(terminator);

            return new SentEvent<T>() {
                @Override
                public Event<T> getEvent() {
                    return event;
                }

                @Override
                public void terminate() {
                    terminator.run();

                    node.remove();
                }
            };
        }

    }

    static class EventImpl<T> implements Event<T> {

        private final T t;
        private ConcurrentDoublyLinkedList<Runnable> _runnables =
            new ConcurrentDoublyLinkedList<>();

        public EventImpl(T t) {
            this.t = t;
        }

        @Override
        public T getContent() {
            return t;
        }

        ConcurrentDoublyLinkedList.Node addTerminator(Runnable terminator) {
            return _runnables.addFirst(terminator);
        }

        void terminate() {
            Runnable runnable;

            while ((runnable = _runnables.poll()) != null) {
                runnable.run();
            }
        }

    }
}
