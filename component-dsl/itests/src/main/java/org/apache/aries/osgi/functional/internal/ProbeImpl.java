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
import org.osgi.framework.BundleContext;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public class ProbeImpl<T> extends OSGiImpl<T> {

    public ProbeImpl() {
        super(new ProbeOperationImpl<>());
    }

    public Function<T, SentEvent<T>> getOperation() {
        return (t) -> {
            ProbeOperationImpl<T> operation = (ProbeOperationImpl<T>) _operation;

            Tuple<T> tuple = Tuple.create(t);

            SentEvent<T> sentEvent = new SentEvent<T>() {
                @Override
                public Event<T> getEvent() {
                    return tuple;
                }

                @Override
                public void terminate() {
                    tuple.terminate();
                }
            };

            if (!operation.closed) {
                operation._op.accept(tuple);
            }

            return sentEvent;
        };
    }

    private static class ProbeOperationImpl<T> implements OSGiOperationImpl<T> {

        BundleContext _bundleContext;
        Consumer<Tuple<T>> _op;
        volatile boolean closed;

        @Override
        public OSGiResultImpl run(
            BundleContext bundleContext, Consumer<Tuple<T>> op) {
            _bundleContext = bundleContext;
            _op = op;

            return new OSGiResultImpl(NOOP, () -> closed = true);
        }
    }

}
