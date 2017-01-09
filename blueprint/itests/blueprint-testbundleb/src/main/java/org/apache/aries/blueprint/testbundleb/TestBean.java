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
package org.apache.aries.blueprint.testbundleb;

public class TestBean {
    private String red;
    private String green;
    private String blue;
    
    public void init(){
        setRed("EMPTY");
        setGreen("EMPTY");
        setBlue("EMPTY");
    }
    
    public String getRed() {
        return red;
    }
    public void setRed(String red) {
        this.red = red;
    }
    public String getGreen() {
        return green;
    }
    public void setGreen(String green) {
        this.green = green;
    }
    public String getBlue() {
        return blue;
    }
    public void setBlue(String blue) {
        this.blue = blue;
    }
    
    public boolean methodToInvoke(String argument){
        if(argument!=null){
            if(argument.equals(red)){
                return true;
            }
            if(argument.equals(green)){
                throw new RuntimeException("MATCHED ON GREEN ("+green+")");
            }
        }
        return false;
    }
}
