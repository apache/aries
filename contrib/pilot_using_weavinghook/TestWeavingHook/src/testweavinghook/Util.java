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
package testweavinghook;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.wiring.BundleWiring;

public class Util {
    static ThreadLocal<ClassLoader> classLoaders = new ThreadLocal<ClassLoader>();
    
    public static void storeContextClassloader() {
        classLoaders.set(Thread.currentThread().getContextClassLoader());
    }
    
    public static void restoreContextClassloader() {
        Thread.currentThread().setContextClassLoader(classLoaders.get());
        classLoaders.set(null);
    }
    
    public static void fixContextClassloader() {
        Thread.currentThread().setContextClassLoader(findClassLoader());
    }
    
    private static ClassLoader findClassLoader() {
        ClassLoader cl = Activator.class.getClassLoader();
        if (!(cl instanceof BundleReference)) {
            return null;
        }
        
        BundleReference br = (BundleReference) cl;      
        for (Bundle b : br.getBundle().getBundleContext().getBundles()) {
            // TODO find the appropriate bundle
            if ("MyServiceImpl".equals(b.getSymbolicName())) {
                BundleWiring bw = b.adapt(BundleWiring.class);
                if (bw != null)
                    return bw.getClassLoader();
            }           
        }
        return null;
    }
}
