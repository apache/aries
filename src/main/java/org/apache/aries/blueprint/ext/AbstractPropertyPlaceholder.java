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
package org.apache.aries.blueprint.ext;

import java.util.Map;

import org.apache.aries.blueprint.ComponentDefinitionRegistryProcessor;

/**
 * Abstract class for property placeholders. Kept for compatibility purposes.
 * See: ARIES-1858 and CAMEL-12570
 *
 * @version $Rev$, $Date$
 */
public abstract class AbstractPropertyPlaceholder implements ComponentDefinitionRegistryProcessor {

    protected abstract Object retrieveValue(String text);

    public abstract Map<String, Object> getDefaultProperties();

}
