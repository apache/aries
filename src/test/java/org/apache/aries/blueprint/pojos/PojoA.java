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
import java.util.Properties;
import java.util.Set;

public class PojoA implements InterfaceA {

    private PojoB pojob;
    private List list;
    private Set set;
    private Map map;
    private Number number;
    private Properties props;
    
    private Object[] array;
    private int[] intArray;
    private Number[] numberArray;

    public PojoA() {
    }

    public PojoA(PojoB pojob) {
        this.pojob = pojob;
    }

    public PojoB getPojob() {
        return pojob;
    }

    public List getList() {
        return list;
    }

    public void setList(List list) {
        this.list = list;
    }

    public Set getSet() {
        return set;
    }

    public void setSet(Set set) {
        this.set = set;
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public Properties getProps() {
        return props;
    }
    
    public void setProps(Properties props) {
        this.props = props;
    }
    
    public void setPojob(PojoB pojob) {
        this.pojob = pojob;
    }

    public void setNumber(Number number) {
        this.number = number;
    }
    
    public Number getNumber() {
        return number;
    }
    
    public void setArray(Object[] array) {
        this.array = array;
    }
    
    public Object[] getArray() {
        return array;
    }
    
    public int[] getIntArray() {
        return intArray;
    }
    
    public void setIntArray(int[] array) {
        intArray = array;
    }
    
    public Number[] getNumberArray() {
        return numberArray;
    }
    
    public void setNumberArray(Number[] numberArray) {
        this.numberArray = numberArray;
    }
    
    public void start() {
        System.out.println("Starting component " + this);
    }

    public void stop() {
        System.out.println("Stopping component " + this);
    }
}
