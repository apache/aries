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
package org.apache.aries.jmx.test.bundleb.api;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.jmx.test.bundleb.impl.B;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * 
 *
 * @version $Rev$ $Date$
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MSF implements ManagedServiceFactory {

    Map<String, InterfaceB> configured = new HashMap<String, InterfaceB>();
    
    public void deleted(String pid) {
       configured.remove(pid);
    }

    public String getName() {
        return "jmx.test.B.factory";
    }

    
	public void updated(String pid, Dictionary dictionary) throws ConfigurationException {
        if (configured.containsKey(pid)) {
            configured.get(pid).configure(dictionary);
        } else {
            InterfaceB ser = new B();
            ser.configure(dictionary);
            configured.put(pid, ser);
        }
    }

    // test cback
    public InterfaceB getConfigured(String pid) {
        return configured.get(pid);
    }
    
}
