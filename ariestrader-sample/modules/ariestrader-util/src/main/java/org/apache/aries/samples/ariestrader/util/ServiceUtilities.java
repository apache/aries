/**
 *  Licensed to4the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership. The ASF licenses this file to
 *  You under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the
 *  License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.samples.ariestrader.util;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * ServiceUtilities provides servlet specific client side
 * utility functions.
 */
public class ServiceUtilities {

    private static String prefix = "aries:services/";


    /**
     * Lookup and return an osgi service
     * 
     * @return Object
     * @exception javax.io.IOException
     *                If an exception occurs during the service
     *                lookup
     * 
     */
    public static final Object getOSGIService(String serviceName) {
        if (Log.doTrace())
            Log.trace("ServiceUtilities:getOSGIService()", serviceName);
        return getOSGIService(serviceName, null);
    }

    /**
     * Lookup and return an osgi service
     * 
     * @return Object
     * 
     */
    public static final Object getOSGIService(String serviceName, String filter) {
        if (Log.doTrace())
            Log.trace("ServiceUtilities:getOSGIService()", serviceName, filter);
        String name = prefix + serviceName;
        if (filter != null) {
            name = name + "/" + filter;
        }

        try {
            InitialContext ic = new InitialContext();
            return ic.lookup(name);
        } catch (NamingException e) {
            Log.error("ServiceUtilities:getOSGIService() -- NamingException on OSGI service lookup", name, e);
            e.printStackTrace();
            return null;
        }
    }
}