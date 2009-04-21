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
package org.apache.geronimo.blueprint.sample;

import java.io.Serializable;
import java.util.Currency;
import java.util.Date;

public class Foo implements Serializable {
    
    private int a;
    private int b;
    private Bar bar;
    private Currency currency;
    private Date date;

    public int getA() {
        return a;
    }

    public int getB() {
        return b;
    }

    public Bar getBar() {
        return bar;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Date getDate() {
        return date;
    }

    public String toString() {
        return a + " " + b + " " + bar + " " + currency + " " + date;
    }

}

