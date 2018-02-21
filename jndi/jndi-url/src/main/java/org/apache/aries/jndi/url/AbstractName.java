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

import javax.naming.CompositeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@SuppressWarnings("serial")
public abstract class AbstractName extends CompositeName {
    public AbstractName(String name) {
        super(split(name));
    }

    protected static Enumeration<String> split(String name) {
        List<String> elements = new ArrayList<String>();

        StringBuilder builder = new StringBuilder();

        int len = name.length();
        int count = 0;

        for (int i = 0; i < len; i++) {
            char c = name.charAt(i);

            if (c == '/' && count == 0) {
                elements.add(builder.toString());
                builder = new StringBuilder();
                continue;
            } else if (c == '(') count++;
            else if (c == ')') count++;

            builder.append(c);
        }

        elements.add(builder.toString());

        return Collections.enumeration(elements);
    }

    public String getScheme() {
        String part0 = get(0);
        int index = part0.indexOf(':');
        if (index > 0) {
            return part0.substring(0, index);
        } else {
            return null;
        }
    }

    public String getSchemePath() {
        String part0 = get(0);
        int index = part0.indexOf(':');

        String result;

        if (index > 0) {
            result = part0.substring(index + 1);
        } else {
            result = null;
        }

        return result;
    }
}
