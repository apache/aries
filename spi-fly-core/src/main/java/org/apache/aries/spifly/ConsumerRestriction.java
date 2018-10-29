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
package org.apache.aries.spifly;

import java.util.Map;

public class ConsumerRestriction {
    private final String className;
    private final MethodRestriction methodRestriction;

    public ConsumerRestriction(String className, MethodRestriction methodRestriction) {
        this.className = className;
        this.methodRestriction = methodRestriction;
    }

    public String getClassName() {
        return className;
    }

    public MethodRestriction getMethodRestriction() {
        return methodRestriction;
    }

    public MethodRestriction getMethodRestriction(String methodName) {
        if (methodName.equals(methodRestriction.getMethodName())) {
            return methodRestriction;
        } else {
            return null;
        }
    }

    public boolean matches(String clsName, String mtdName, Map<Pair<Integer, String>, String> args) {
        if (!className.equals(clsName))
            return false;
        
        return methodRestriction.matches(mtdName, args);
    }
}
