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

package org.apache.aries.blueprint.test;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * Default class resolver that uses regular class loader to load classes.
 */
public class DefaultClassResolver implements ClassResolver {

    public DefaultClassResolver() { }


    public InputStream loadResourceAsStream(String uri) {
        ObjectHelper.notEmpty(uri, "uri");
        return ObjectHelper.loadResourceAsStream(uri, DefaultClassResolver.class.getClassLoader());
    }

    public URL loadResourceAsURL(String uri) {
        ObjectHelper.notEmpty(uri, "uri");
        return ObjectHelper.loadResourceAsURL(uri, DefaultClassResolver.class.getClassLoader());
    }

    public Enumeration<URL> loadResourcesAsURL(String uri) {
        return loadAllResourcesAsURL(uri);
    }

    public Enumeration<URL> loadAllResourcesAsURL(String uri) {
        ObjectHelper.notEmpty(uri, "uri");
        return ObjectHelper.loadResourcesAsURL(uri);
    }

}
