/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jpa.container.unit.impl;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javax.persistence.spi.ClassTransformer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * @version $Rev$ $Date$
 */
public class TransformerWrapper implements ClassFileTransformer {

    private final ClassTransformer classTransformer;
    private final Bundle bundle;

    public TransformerWrapper(ClassTransformer classTransformer, Bundle bundle) {
        this.classTransformer = classTransformer;
        this.bundle = bundle;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (loader instanceof BundleReference && ((BundleReference) loader).getBundle() == bundle) {
            try {
                byte[] rt = classTransformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
                return rt;
            } catch (IllegalClassFormatException e) {
                throw e;
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;

    }
}
