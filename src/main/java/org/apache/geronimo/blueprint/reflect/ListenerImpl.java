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
package org.apache.geronimo.blueprint.reflect;

import org.apache.geronimo.blueprint.mutable.MutableListener;
import org.osgi.service.blueprint.reflect.Listener;
import org.osgi.service.blueprint.reflect.Target;

/**
 * Implementation of Listener
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ListenerImpl implements MutableListener {

    private Target listenerComponent;
    private String bindMethodName;
    private String unbindMethodName;

    public ListenerImpl() {
    }

    public ListenerImpl(Target listenerComponent, String bindMethodName, String unbindMethodName) {
        this.listenerComponent = listenerComponent;
        this.bindMethodName = bindMethodName;
        this.unbindMethodName = unbindMethodName;
    }

    public ListenerImpl(Listener source) {
        this.listenerComponent = MetadataUtil.cloneTarget(source.getListenerComponent());
        this.bindMethodName = source.getBindMethodName();
        this.unbindMethodName = source.getUnbindMethodName();
    }

    public Target getListenerComponent() {
        return this.listenerComponent;
    }

    public void setListenerComponent(Target listenerComponent) {
        this.listenerComponent = listenerComponent;
    }

    public String getBindMethodName() {
        return this.bindMethodName;
    }

    public void setBindMethodName(String bindMethodName) {
        this.bindMethodName = bindMethodName;
    }

    public String getUnbindMethodName() {
        return this.unbindMethodName;
    }

    public void setUnbindMethodName(String unbindMethodName) {
        this.unbindMethodName = unbindMethodName;
    }

    @Override
    public String toString() {
        return "Listener[" +
                "listenerComponent=" + listenerComponent +
                ", bindMethodName='" + bindMethodName + '\'' +
                ", unbindMethodName='" + unbindMethodName + '\'' +
                ']';
    }
}
