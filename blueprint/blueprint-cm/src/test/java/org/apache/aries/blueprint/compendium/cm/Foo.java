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
package org.apache.aries.blueprint.compendium.cm;

import java.util.Map;
import java.util.Properties;

public class Foo implements FooInterface {

    public Foo() {
    }

    private int a;
    private String b;
    private Properties props;

    public int getA() {
        return a;
    }

    public void setA(int i) {
        a = i;
    }

    public String getB() {
        return b;
    }

    public void setB(String i) {
        b = i;
    }

    public Properties getProps() {
        return props;
    }

    public void setProps(Properties props) {
        this.props = props;
    }
    
  public void update(Map<String, String> pMap) {
    Properties properties = new Properties();

    String value = pMap.get("a");
    if (value != null) {
      a = Integer.parseInt(value);
      properties.put("a", a);
    }

    value = pMap.get("b");
    if (value != null) {
      b = value;
      properties.put("b", b);
    }

    props = properties;
  }
}

