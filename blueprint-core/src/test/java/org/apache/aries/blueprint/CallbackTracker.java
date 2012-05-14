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
package org.apache.aries.blueprint;

import java.util.ArrayList;
import java.util.List;

public class CallbackTracker {

    private static List<Callback> callbacks = new ArrayList<Callback>();
    
    public static void add(Callback callback) {
        callbacks.add(callback);
    }
    
    public static List<Callback> getCallbacks() {
        return callbacks;
    }
    
    public static void clear() {
        callbacks.clear();
    }
    
    public static class Callback {
        
        public static int INIT = 1;
        public static int DESTROY = 2;
        
        private Object object;
        private int type;
        
        public Callback(int type, Object object) {
            this.type = type;
            this.object = object;
        }        
        
        public int getType() {
            return type;
        }
        
        public Object getObject() {
            return object;
        }
        
        public String toString() {
            return type + " " + object;
        }
        
    }
}
