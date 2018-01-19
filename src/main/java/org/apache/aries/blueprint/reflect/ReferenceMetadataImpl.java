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
package org.apache.aries.blueprint.reflect;

import java.util.Collection;
import java.util.Collections;

import org.apache.aries.blueprint.mutable.MutableReferenceMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;

/**
 * Implementation of ReferenceMetadata
 *
 * @version $Rev$, $Date$
 */
public class ReferenceMetadataImpl extends ServiceReferenceMetadataImpl implements MutableReferenceMetadata {

    private long timeout;
    private String defaultBeanId;
    private Collection<Class<?>> proxyChildBeanClasses;
    private Collection<String> extraInterfaces;
    private int damping;
    private int lifecycle;

    public ReferenceMetadataImpl() {
    }
    
    public ReferenceMetadataImpl(ReferenceMetadata source) {
        super(source);
        timeout = source.getTimeout();
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setDefaultBean(String defaultBeanId) {
      this.defaultBeanId = defaultBeanId;
    }

    public String getDefaultBean() {
      return defaultBeanId;
    }

    @Override
    public String toString() {
        return "ReferenceMetadata[" +
                "id='" + id + '\'' +
                ", activation=" + activation +
                ", dependsOn=" + dependsOn +
                ", availability=" + availability +
                ", interface='" + interfaceName + '\'' +
                ", componentName='" + componentName + '\'' +
                ", filter='" + filter + '\'' +
                ", referenceListeners=" + referenceListeners +
                ", timeout=" + timeout +
                ", additonalInterfaces=" + getExtraInterfaces() +
                ", damping=" + getDamping() +
                ", lifecycle=" + getLifecycle() +
                ']';
    }

    public Collection<Class<?>> getProxyChildBeanClasses() {
        return proxyChildBeanClasses;
    }

    public void setProxyChildBeanClasses(Collection<Class<?>> c) {
        proxyChildBeanClasses = c;
    }

    public Collection<String> getExtraInterfaces() {
        if (extraInterfaces == null) {
            return Collections.emptyList();
        }
        return extraInterfaces;
    }

    public void setExtraInterfaces(Collection<String> interfaces) {
        extraInterfaces = interfaces;
    }

    public int getDamping() {
        return damping;
    }

    public void setDamping(int damping) {
        this.damping = damping;
    }

    public int getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(int lifecycle) {
        this.lifecycle = lifecycle;
    }
}
