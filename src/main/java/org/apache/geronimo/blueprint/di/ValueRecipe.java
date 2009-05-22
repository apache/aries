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
package org.apache.geronimo.blueprint.di;

import java.lang.reflect.Type;

import org.osgi.service.blueprint.reflect.ValueMetadata;

/**
 * This recipe will be used to create an object from a ValueMetadata.
 * We need to keep the reference to the ValueMetadata so that we can lazily retrieve
 * the value, allowing for placeholders or such to be used at the last moment.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev$, $Date$
 */
public class ValueRecipe extends AbstractRecipe {

    private ValueMetadata value;
    private Object type;

    public ValueRecipe(ValueMetadata value, Object type) {
        this.value = value;
        this.type = type;
    }

    @Override
    protected Object internalCreate(boolean lazyRefAllowed) throws ConstructionException {
        try {
            Type type = Object.class;
            if (this.type instanceof Type) {
                type = (Type) this.type;
            } else if (this.type instanceof String) {
                type = loadClass((String) this.type);
            }
            return convert(value.getStringValue(), type);
        } catch (Exception e) {            
            throw new ConstructionException(e);
        }
    }

    public String toString() {
        return "ValueRecipe: " + type + " " + value.getStringValue();
    }

}
