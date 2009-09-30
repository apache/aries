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
package org.apache.aries.blueprint.pojos;

import java.util.Map;
import java.util.Properties;

public class Multiple {

    private int intValue = -1;
    private Integer integerValue = null;
    private String stringValue = null;
    private Map map;
    private Properties properties;

    public Multiple() {
    }

    public Multiple(Map map) {
        this.map = map;
    }

    public Multiple(Properties props) {
        this.properties = props;
    }

    public Multiple(String arg) {   
        stringValue = arg;
    }

    public Multiple(int arg) {   
        intValue = arg;
    }
    
    public Multiple(Integer arg) {   
        integerValue = arg;
    }

    public Map getMap() {
        return map;
    }

    public Properties getProperties() {
        return properties;
    }

    public int getInt() {
        return intValue;
    }
    
    public Integer getInteger() {
        return integerValue;
    }

    public String getString() {
        return stringValue;
    }

    public void setAmbiguous(int v) {
    }

    public void setAmbiguous(Object v) {
    }

    public static Multiple create(String arg1, Integer arg2) {
        return new Multiple(arg2.intValue());
    }
    
    public static Multiple create(String arg1, Boolean arg2) {
        return new Multiple(arg1 + "-boolean");
    }
    
}
