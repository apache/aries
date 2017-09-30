/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.handlers.blueprint.service;

import org.apache.aries.blueprint.annotation.service.Reference;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;

import static org.apache.aries.blueprint.plugin.handlers.blueprint.service.ReferenceParameters.needAvailability;
import static org.apache.aries.blueprint.plugin.handlers.blueprint.service.ReferenceParameters.needTimeout;

class ReferenceId {
    static String generateReferenceId(Class clazz, Reference reference, ContextEnricher contextEnricher) {
        StringBuilder sb = new StringBuilder();
        writeBeanNameFromSimpleName(sb, clazz.getSimpleName());
        sb.append("-");
        if (!"".equals(reference.filter())) {
            writeEscapedFilter(sb, reference.filter());
        }
        sb.append("-");
        if (!"".equals(reference.componentName())) {
            sb.append(reference.componentName());
        }
        sb.append("-");
        if (needAvailability(contextEnricher, reference)) {
            sb.append(reference.availability().name().toLowerCase());
        }
        sb.append("-");
        if (needTimeout(reference)) {
            sb.append(reference.timeout());
        }
        return sb.toString().replaceAll("-+$", "");
    }

    private static void writeBeanNameFromSimpleName(StringBuilder sb, String name) {
        sb.append(name.substring(0, 1).toLowerCase());
        sb.append(name.substring(1, name.length()));
    }

    private static void writeEscapedFilter(StringBuilder sb, String filter) {
        for (int c = 0; c < filter.length(); c++) {
            char ch = filter.charAt(c);
            if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9') {
                sb.append(ch);
            }
        }
    }
}
