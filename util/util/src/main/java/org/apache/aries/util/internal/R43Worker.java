/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.aries.util.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

/**
 * @version $Rev$ $Date$
 */
public class R43Worker implements FrameworkUtilWorker {

    static {
        BundleWiring.class.getClassLoader();
    }

    public ClassLoader getClassLoader(Bundle b) {
    	//This cast should be unnecessary, but without it I get a compile failure 
    	//when using Maven 3.0.4, Java 7.0.21 and MacOSX 10.8.3
        return ((BundleWiring)b.adapt(BundleWiring.class)).getClassLoader();
    }

    public boolean isValid() {
        return true;
    }
}
