/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.blueprint.utils;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * @version $Rev$ $Date$
 */
public final class JavaUtils {

    private JavaUtils() {
    }

    public static void copy(Dictionary destination, Dictionary source) {
        Enumeration e = source.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            Object value = source.get(key);
            destination.put(key, value);
        }
    }

    public static Hashtable getProperties(ServiceReference ref) {
        Hashtable props = new Hashtable();
        for (String key : ref.getPropertyKeys()) {
            props.put(key, ref.getProperty(key));
        }
        return props;
    }

    public static Version getBundleVersion(Bundle bundle) {
        Dictionary headers = bundle.getHeaders();
        String version = (String) headers.get(Constants.BUNDLE_VERSION);
        return (version != null) ? Version.parseVersion(version) : Version.emptyVersion;
    }

}
