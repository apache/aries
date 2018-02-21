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
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * A parser for the aries namespace
 */
public final class BlueprintNameParser implements NameParser {
    private static final String BLUEPRINT_SCHEME = "blueprint";
    private static final String COMP_PATH = "comp";

    @Override
    public Name parse(String name) throws NamingException {
        BlueprintName result = new BlueprintName(name);
        String urlScheme = result.getScheme();
        String schemePath = result.getSchemePath();

        if (!BLUEPRINT_SCHEME.equals(urlScheme) || !COMP_PATH.equals(schemePath)) {
            throw new InvalidNameException(name);
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OsgiNameParser;
    }

    @Override
    public int hashCode() {
        return 100004;
    }
}