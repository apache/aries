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

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PojoGenerics {

    private List<Integer> list;
    private Set<Long> set;
    private Map<Short, Boolean> map;

    public PojoGenerics() {
    }
    
    public PojoGenerics(List<Integer> list) {
        this.list = list;        
    }
    
    public PojoGenerics(Set<Long> set) {
        this.set = set;        
    }
    
    public PojoGenerics(Map<Short, Boolean> map) {
        this.map = map;
    }

    public List<Integer> getList() {
        return list;
    }

    public void setList(List<Integer> list) {
        this.list = list;
    }

    public Set<Long> getSet() {
        return set;
    }

    public void setSet(Set<Long> set) {
        this.set = set;
    }

    public Map<Short, Boolean> getMap() {
        return map;
    }

    public void setMap(Map<Short, Boolean> map) {
        this.map = map;
    }
     
    public void start() {
        System.out.println("Starting component " + this);
    }

    public void stop() {
        System.out.println("Stopping component " + this);
    }
}
