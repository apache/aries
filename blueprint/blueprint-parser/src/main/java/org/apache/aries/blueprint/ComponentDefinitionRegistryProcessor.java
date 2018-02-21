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
package org.apache.aries.blueprint;

/**
 * A processor that processes Blueprint component definitions after they have been parsed but before
 * component managers are created.
 * 
 * Component definition registry processors must be advertised as such in the blueprint xml. Do this by using
 * the custom attribute defined in the extension schema.
 * <pre>
 *    &lt;bp:bean ext:role="processor" ...&gt;
 * </pre>
 * 
 * When a definition registry processor is invoked type converters and registry processors have been already
 * been created. Hence, changing component definitions for these or any components referenced by them will have 
 * no effect.
 * 
 * Note: a processor that replaces existing component definitions with new ones should take care to copy
 * interceptors defined against the old component definition if appropriate
 * 
 * @version $Rev$, $Date$
 */
public interface ComponentDefinitionRegistryProcessor {

    /**
     * Process a <code>ComponentDefinitionRegistry</code>
     * @param registry 
     */
    void process(ComponentDefinitionRegistry registry);

}
