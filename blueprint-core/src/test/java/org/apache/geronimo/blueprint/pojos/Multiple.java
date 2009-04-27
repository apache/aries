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
package org.apache.geronimo.blueprint.pojos;

public class Multiple {

    private int intValue = -1;
    private String stringValue = null;

    public Multiple(String arg) {   
        stringValue = arg;
    }

    public Multiple(int arg) {   
        intValue = arg;
    }

    public int getInt() {
        return intValue;
    }

    public String getString() {
        return stringValue;
    }
    
    public static Multiple create(String arg1, Integer arg2) {
        return new Multiple(arg2.intValue());
    }
    
    public static Multiple create(String arg1, Boolean arg2) {
        return new Multiple(arg1 + "-boolean");
    }
    
}
