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
package org.apache.aries.blueprint.sample;

import java.util.List;
import java.util.Map;

import org.apache.aries.blueprint.annotation.Bean;
import org.apache.aries.blueprint.annotation.Bind;
import org.apache.aries.blueprint.annotation.Init;
import org.apache.aries.blueprint.annotation.Inject;
import org.apache.aries.blueprint.annotation.ReferenceList;
import org.apache.aries.blueprint.annotation.ReferenceListener;
import org.apache.aries.blueprint.annotation.Unbind;
import org.osgi.framework.ServiceReference;

@Bean(id="listBindingListener")
@ReferenceListener
public class ListBindingListener {

    @Inject @ReferenceList (id="ref-list", 
            serviceInterface = InterfaceA.class,
            referenceListeners=@ReferenceListener(ref="listBindingListener"))
    private InterfaceA a;
    private Map props;
    private ServiceReference reference;
    private List list;

    public InterfaceA getA() {
        return a;
    }

    public Map getProps() {
        return props;
    }

    public ServiceReference getReference() {
        return reference;
    }

    public List getList() {
        return list;
    }

    public void setList(List list) {
        this.list = list;
    }

    @Init
    public void init() {
    }

    @Bind
    public void bind(InterfaceA a, Map props) {
        this.a = a;
        this.props = props;
    }

    @Bind
    public void bind(ServiceReference ref) {
        this.reference = ref;
    }

    @Unbind
    public void unbind(InterfaceA a, Map props) {
        this.a = null;
        this.props = null;
    }

    @Unbind
    public void unbind(ServiceReference ref) {
        this.reference = null;
    }
}
