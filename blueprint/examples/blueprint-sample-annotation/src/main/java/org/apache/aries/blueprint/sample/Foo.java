/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.sample;

import java.io.Serializable;
import java.util.Currency;
import java.util.Date;
import java.util.Map;

import org.apache.aries.blueprint.annotation.Bean;
import org.apache.aries.blueprint.annotation.Destroy;
import org.apache.aries.blueprint.annotation.Init;
import org.apache.aries.blueprint.annotation.Inject;
import org.apache.aries.blueprint.annotation.RegistrationListener;
import org.apache.aries.blueprint.annotation.Service;
import org.apache.aries.blueprint.annotation.ServiceProperty;

@Bean(id="foo")
@Service(autoExport="all-classes",
        serviceProperties = @ServiceProperty(key="blueprint.annotation.sample", value="true"),
        registerationListeners = @RegistrationListener(ref="fooRegistrationListener"), 
        ranking=0)
public class Foo implements Serializable {
    
    @Inject(value="5")
    private int a;
    
    //@Inject(value="${key.b}")
    private int b;
    
    //@Inject(ref="bar")
    private Bar bar;
    
    @Inject(value="PLN")
    private Currency currency;
    
    @Inject(value="2009.04.17")
    private Date date;

    public boolean initialized;
    public boolean destroyed;
    private Map<String, Object> props;

    public int getA() {
        return a;
    }

    public void setA(int i) {
        a = i;
    }

    public int getB() {
        return b;
    }

    public void setB(int i) {
        b = i;
    }

    public Bar getBar() {
        return bar;
    }

    @Inject(ref="bar")
    public void setBar(Bar b) {
        bar = b;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency c) {
        currency = c;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date d) {
        date = d;
    }

    public String toString() {
        return a + " " + b + " " + bar + " " + currency + " " + date;
    }

    @Init
    public void init() {
        System.out.println("======== Initializing Foo =========");
        initialized = true;
    }

    @Destroy
    public void destroy() {
        System.out.println("======== Destroying Foo =========");
        destroyed = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void update(Map<String,Object> props) {
        this.props = props;
    }

    public Map<String, Object> getProps() {
        return props;
    }
}

