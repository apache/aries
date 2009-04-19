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

import org.osgi.service.blueprint.reflect.ArrayValue;
import org.osgi.service.blueprint.reflect.Value;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ArrayValueImpl implements ArrayValue {

    private Value[] array;
    private String valueType;

    public ArrayValueImpl() {
    }

    public ArrayValueImpl(String valueType, Value[] array) {
        this.array = array;
        this.valueType = valueType;
    }

    public ArrayValueImpl(ArrayValue source) {
        Value[] valueArray = source.getArray();

        array = new Value[valueArray.length];
        for (int i = 0; i < valueArray.length; i++) {
            array[i] = MetadataUtil.cloneValue(valueArray[i]);
        }

        valueType = source.getValueType();
    }
    
    public Value[] getArray() {
        return array;
    }

    public void setArray(Value[] array) {
        this.array = array;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }
}
