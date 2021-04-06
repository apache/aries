/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.spifly.itests.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TeeOutputStream extends OutputStream {

    final OutputStream[]    out;
    final Thread            thread;

    public TeeOutputStream(OutputStream... out) {
        this(null, out);
    }

    public TeeOutputStream(Thread thread, OutputStream... out) {
        this.thread = thread;
        this.out = out;
    }

    @Override
    public void write(int b) throws IOException {
        if (thread == null || Thread.currentThread() == thread)
            for (OutputStream o : out) {
                o.write(b);
            }
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (thread == null || Thread.currentThread() == thread)
            for (OutputStream o : out) {
                o.write(b);
            }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (thread == null || Thread.currentThread() == thread)
            for (OutputStream o : out) {
                o.write(b, off, len);
            }
    }

    public void close() throws IOException {
        if (out == null)
            return;

        List<Throwable> exceptions = new ArrayList<>();
        for (Object o : out) {
            if (o instanceof AutoCloseable) {
                close((AutoCloseable) o).ifPresent(exceptions::add);
            } else if (o instanceof Iterable) {
                for (Object oo : (Iterable<?>) o) {
                    if (oo instanceof AutoCloseable) {
                        // do not recurse!
                        close((AutoCloseable) oo).ifPresent(exceptions::add);
                    }
                }
            }
        }
        if (!exceptions.isEmpty()) {
            IOException ioe = new IOException();
            exceptions.stream().forEach(ioe::addSuppressed);
            throw ioe;
        }
    }

    private Optional<Throwable> close(AutoCloseable in) {
        try {
            if (in != null)
                in.close();
        } catch (Throwable e) {
            return Optional.of(e);
        }
        return Optional.empty();
    }

}
