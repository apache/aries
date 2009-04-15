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
package org.apache.felix.blueprint.reflect;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.osgi.service.blueprint.reflect.SetValue;
import org.osgi.service.blueprint.reflect.Value;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class SetValueImpl implements SetValue {

    private String valueType;
    private Set<Value> set;

    public SetValueImpl() {
    }

    public SetValueImpl(String valueType, Set<Value> set) {
        this.valueType = valueType;
        this.set = set;
    }

    public SetValueImpl(SetValue source) {
        if (source.getSet() != null) {
            set = new HashSet<Value>();
            Iterator i = source.getSet().iterator();
            while (i.hasNext()) {
                set.add(MetadataUtil.cloneValue((Value)i.next()));
            }
        }
        valueType = source.getValueType();
    }
    
    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public Set<Value> getSet() {
        return set;
    }

    public void setSet(Set<Value> set) {
        this.set = set;
    }
}
