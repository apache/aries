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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.osgi.service.blueprint.reflect.ListValue;
import org.osgi.service.blueprint.reflect.Value;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ListValueImpl implements ListValue {

    private String valueType;
    private List<Value> list;

    public ListValueImpl() {
    }

    public ListValueImpl(String valueType, List<Value> list) {
        this.valueType = valueType;
        this.list = list;
    }
    
    public ListValueImpl(ListValue source) {
        if (source.getList() != null) {
            list = new ArrayList<Value>();
            Iterator i = source.getList().iterator();
            while (i.hasNext()) {
                list.add(MetadataUtil.cloneValue((Value)i.next()));
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

    public List<Value> getList() {
        return list;
    }

    public void setList(List<Value> list) {
        this.list = list;
    }

}
