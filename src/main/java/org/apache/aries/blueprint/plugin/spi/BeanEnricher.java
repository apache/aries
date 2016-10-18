/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.spi;

/**
 * Interface allows for adding custom XML or adding attributes to bean element
 * Instance of this interface is provided by plugin.
 */
public interface BeanEnricher {
    /**
     * Add attribute to bean element
     * @param key name of attribute
     * @param value value of attribute
     */
    void addAttribute(String key, String value);

    /**
     * Add custom XML inside bean element
     * @param id identifier of writer instance (should be unique)
     * @param blueprintWriter callback used to write custom XML
     */
    void addBeanContentWriter(String id, XmlWriter blueprintWriter);
}
