/** 
  * Licensed to the Apache Software Foundation (ASF) under one 
  * or more contributor license agreements. See the NOTICE file 
  * distributed with this work for additional information 
  * regarding copyright ownership. The ASF licenses this file 
  * to you under the Apache License, Version 2.0 (the 
  * "License"); you may not use this file except in compliance 
  * with the License. You may obtain a copy of the License at 
  * 
  * http://www.apache.org/licenses/LICENSE-2.0 
  * 
  * Unless required by applicable law or agreed to in writing, 
  * software distributed under the License is distributed on an 
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
  * KIND, either express or implied. See the License for the 
  * specific language governing permissions and limitations 
  * under the License. 
  */
package org.apache.cxf.dosgi.samples.greeter.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.dosgi.samples.greeter.GreeterData;
import org.apache.cxf.dosgi.samples.greeter.GreeterException;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.GreetingPhrase;

public class GreeterServiceImpl implements GreeterService {
    public Map<GreetingPhrase, String> greetMe(String name) {
        System.out.println("Invoking: greetMe(" + name + ")");
        
        Map<GreetingPhrase, String> greetings = 
            new HashMap<GreetingPhrase, String>();
        
        greetings.put(new GreetingPhrase("Hello"), name);
        greetings.put(new GreetingPhrase("Hoi"), name);
        greetings.put(new GreetingPhrase("Hola"), name);
        greetings.put(new GreetingPhrase("Bonjour"), name);
        
        
        return greetings;
    }

    public GreetingPhrase [] greetMe(GreeterData gd) throws GreeterException {
        if (gd.isException()) {
            System.out.println("Throwing custom exception from: greetMe(" + gd.getName() + ")");
            throw new GreeterException(gd.getName());
        }
        
        String details = gd.getName() + "(" + gd.getAge() + ")";
        System.out.println("Invoking: greetMe(" + details + ")");
        
        GreetingPhrase [] greetings = new GreetingPhrase [] {
            new GreetingPhrase("Howdy " + details),
            new GreetingPhrase("Hallo " + details),
            new GreetingPhrase("Ni hao " + details)
        };
        
        return greetings;
    }
}
