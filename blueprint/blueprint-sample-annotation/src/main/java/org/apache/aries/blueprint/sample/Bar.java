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

import org.osgi.framework.BundleContext;
import org.apache.aries.blueprint.annotation.Blueprint;
import org.apache.aries.blueprint.annotation.Bean;
import org.apache.aries.blueprint.annotation.Inject;
import org.apache.aries.blueprint.annotation.Element;
import org.apache.aries.blueprint.annotation.Element.ElementType;

@Blueprint(defaultActivation="eager", defaultTimeout=300, defaultAvailability="optional")
@Bean(id="bar")
public class Bar {
    
    @Inject(value="Hello FooBar")
    private String value;

    @Inject(ref="blueprintBundleContext")
    private BundleContext context;

    /*@Inject 
    @org.apache.aries.blueprint.annotation.List ({ 
        @Element(value="a list element"), 
        @Element(value="5", type=ElementType.INTEGER) 
    })*/
    private List list;

    public BundleContext getContext() {
        return context;
    }

    public void setContext(BundleContext ctx) {
        context = ctx;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String s) {
        value = s;
    }

    public List getList() {
        return list;
    }

    public void setList(List l) {
        list = l;
    }

    public String toString() {
        return hashCode() + ": " + value + " " + context + " " + list;
    }

}
