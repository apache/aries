/**
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
package org.apache.aries.blueprint.plugin.model;

import com.google.common.base.Objects;

public class TransactionalDef {
    public static final String TYPE_REQUIRED = "Required";
    public static final String TYPE_REQUIRES_NEW = "RequiresNew";
    private String method;
    private String type;

    public TransactionalDef(String method, String type) {
        this.method = method;
        this.type = type;
    }

    public String getMethod() {
        return method;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        TransactionalDef that = (TransactionalDef) o;
        return Objects.equal(method, that.method) && Objects.equal(type, that.type);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(method, type);
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this).add("method", method).add("type", type).toString();
    }
}
