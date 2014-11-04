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
package org.apache.aries.web.converter.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.aries.web.converter.WarToWabConverter;
import org.osgi.framework.Constants;

/**
 * Simple key case-insensitive map where only selected set of keys are 
 * treated in case-insensitive way.
 */
@SuppressWarnings("serial")
public class CaseInsensitiveMap extends HashMap<String, String> {
    
    private static final Map<String, String> DEFAULT_KEY_MAP = new HashMap<String, String>();
    static {
        addKeyMapping(DEFAULT_KEY_MAP, Constants.BUNDLE_SYMBOLICNAME);
        addKeyMapping(DEFAULT_KEY_MAP, Constants.BUNDLE_VERSION);
        addKeyMapping(DEFAULT_KEY_MAP, Constants.BUNDLE_MANIFESTVERSION);
        addKeyMapping(DEFAULT_KEY_MAP, Constants.IMPORT_PACKAGE);
        addKeyMapping(DEFAULT_KEY_MAP, Constants.BUNDLE_CLASSPATH);
        addKeyMapping(DEFAULT_KEY_MAP, WarToWabConverter.WEB_CONTEXT_PATH);
    }
    
    private static void addKeyMapping(Map<String, String> mappings, String name) {
        mappings.put(name.toLowerCase(), name);
    }
    
    private Map<String, String> keyMap;
    
    public CaseInsensitiveMap() {
        this.keyMap = new HashMap<String, String>(DEFAULT_KEY_MAP);
    }
    
    public CaseInsensitiveMap(Map<String, String> source) {
        this();
        putAll(source);
    }
    
    public CaseInsensitiveMap(Properties source) {
        this();
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            put(key, value);
        }
    }
    
    @Override
    public String put(String name, String value) {
        return super.put(getMappedName(name), value);
    }
    
    @Override
    public String get(Object name) {
        if (!(name instanceof String)) {
            return null;
        }
        return super.get(getMappedName((String) name));
    }
    
    @Override
    public boolean containsKey(Object name) {
        if (!(name instanceof String)) {
            return false;
        }
        return super.containsKey(getMappedName((String) name));
    }
    
    private String getMappedName(String name) {
        String mappedName = keyMap.get(name.toLowerCase());
        if (mappedName == null) {
            mappedName = name;
        }
        return mappedName;
    }
}

