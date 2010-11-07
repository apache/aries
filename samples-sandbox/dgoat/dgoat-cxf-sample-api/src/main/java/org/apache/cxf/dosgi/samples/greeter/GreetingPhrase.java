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
package org.apache.cxf.dosgi.samples.greeter;

public class GreetingPhrase {
    private String phrase;
    
    public GreetingPhrase() {
    }
    
    public GreetingPhrase(String phrase) {
        this.phrase = phrase;
    }

    public void setPhrase(String thePhrase) {
        this.phrase = thePhrase;
    }
    
    public String getPhrase() {
        return phrase;
    }
    
    @Override
    public int hashCode() {
        return phrase.hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        if (!GreetingPhrase.class.isAssignableFrom(other.getClass())) {
            return false;
        }
        
        return phrase.equals(((GreetingPhrase)other).phrase);
    }
}
