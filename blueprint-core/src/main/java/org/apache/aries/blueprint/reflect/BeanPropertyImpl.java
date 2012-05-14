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

import org.apache.aries.blueprint.mutable.MutableBeanProperty;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.Metadata;

/**
 * Implementation of BeanProperty
 *
 * @version $Rev$, $Date$
 */
public class BeanPropertyImpl implements MutableBeanProperty {

    private String name;
    private Metadata value;

    public BeanPropertyImpl() {
    }

    public BeanPropertyImpl(String name, Metadata value) {
        this.name = name;
        this.value = value;
    }

    public BeanPropertyImpl(BeanProperty source) {
        this.name = source.getName();
        this.value = MetadataUtil.cloneMetadata(source.getValue());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Metadata getValue() {
        return value;
    }

    public void setValue(Metadata value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "BeanProperty[" +
                "name='" + name + '\'' +
                ", value=" + value +
                ']';
    }
}