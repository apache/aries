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
package org.apache.aries.blueprint;

/**
 * Additional namespace features
 */
public interface NamespaceHandler2 extends NamespaceHandler {

    /**
     * A namespace can return true if its parsing relies on PSVI,
     * i.e. extensions from the schema for default attributes values
     * for example.
     */
    boolean usePsvi();

    /**
     * <p>A hint for a registry of handlers that this handler actually may resolve given namespace
     * and {@link NamespaceHandler#getSchemaLocation(String) return a location for XSD resource} for it.</p>
     * <p>Some handlers return <em>some</em> XSD resource when asked for location of unknown namespace</p>
     * @param namespace
     * @return
     */
    boolean mayResolve(String namespace);

}
