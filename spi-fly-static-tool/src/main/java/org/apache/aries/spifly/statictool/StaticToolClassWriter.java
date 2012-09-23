/*
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
package org.apache.aries.spifly.statictool;

import org.objectweb.asm.ClassWriter;

/**
 * We need to override ASM's default behaviour in
 * {@link #getCommonSuperClass(String, String)} so that it accepts a custom
 * classloader that can also see the jar that is being processed.
 */
public final class StaticToolClassWriter extends ClassWriter {

    private static final String OBJECT_INTERNAL_NAME = "java/lang/Object";
    private final ClassLoader loader;

    public StaticToolClassWriter(int flags, ClassLoader loader) {
        super(flags);

        this.loader = loader;
    }

    /**
     * The implementation uses the classloader provided using the Constructor.
     *
     * This is a slight variation on ASM's default behaviour as that obtains the
     * classloader to use ASMs classloader.
     */
    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        Class<?> c, d;
        try {
            c = Class.forName(type1.replace('/', '.'), false, loader);
            d = Class.forName(type2.replace('/', '.'), false, loader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (c.isAssignableFrom(d)) {
            return type1;
        }
        if (d.isAssignableFrom(c)) {
            return type2;
        }

        if (c.isInterface() || d.isInterface()) {
            return OBJECT_INTERNAL_NAME;
        }

        do {
            c = c.getSuperclass();
        } while (!c.isAssignableFrom(d));

        return c.getName().replace('.', '/');
    }
}
