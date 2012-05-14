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

import org.apache.aries.blueprint.mutable.MutableReferenceListener;
import org.osgi.service.blueprint.reflect.ReferenceListener;
import org.osgi.service.blueprint.reflect.Target;

/**
 * Implementation of Listener
 *
 * @version $Rev$, $Date$
 */
public class ReferenceListenerImpl implements MutableReferenceListener {

    private Target listenerComponent;
    private String bindMethod;
    private String unbindMethod;

    public ReferenceListenerImpl() {
    }

    public ReferenceListenerImpl(Target listenerComponent, String bindMethod, String unbindMethod) {
        this.listenerComponent = listenerComponent;
        this.bindMethod = bindMethod;
        this.unbindMethod = unbindMethod;
    }

    public ReferenceListenerImpl(ReferenceListener source) {
        this.listenerComponent = MetadataUtil.cloneTarget(source.getListenerComponent());
        this.bindMethod = source.getBindMethod();
        this.unbindMethod = source.getUnbindMethod();
    }

    public Target getListenerComponent() {
        return this.listenerComponent;
    }

    public void setListenerComponent(Target listenerComponent) {
        this.listenerComponent = listenerComponent;
    }

    public String getBindMethod() {
        return this.bindMethod;
    }

    public void setBindMethod(String bindMethodName) {
        this.bindMethod = bindMethodName;
    }

    public String getUnbindMethod() {
        return this.unbindMethod;
    }

    public void setUnbindMethod(String unbindMethodName) {
        this.unbindMethod = unbindMethodName;
    }

    @Override
    public String toString() {
        return "Listener[" +
                "listenerComponent=" + listenerComponent +
                ", bindMethodName='" + bindMethod + '\'' +
                ", unbindMethodName='" + unbindMethod + '\'' +
                ']';
    }
}
