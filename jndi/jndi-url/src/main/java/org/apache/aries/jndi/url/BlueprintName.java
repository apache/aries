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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.jndi.url;

import javax.naming.InvalidNameException;
import javax.naming.Name;

public class BlueprintName extends AbstractName {
    /**
     *
     */
    private static final long serialVersionUID = 7460901600614300179L;

    public BlueprintName(String name) throws InvalidNameException {
        super(name);
    }

    public BlueprintName(Name name) throws InvalidNameException {
        this(name.toString());
    }

    public String getComponentId() {
        return get(1);
    }

    public boolean hasComponent() {
        return size() > 1;
    }
}
