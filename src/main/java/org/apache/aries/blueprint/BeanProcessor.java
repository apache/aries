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

import org.osgi.service.blueprint.reflect.BeanMetadata;

/**
 * TODO: javadoc
 *
 * Processors must be advertized as being such.  This can be done by using
 * the custom attribute defined in the extension schema.
 * <pre>
 *    &lt;bp:bean ext:role="processor" ...&gt;
 * </pre>
 *
 * @version $Rev$, $Date$
 */
public interface BeanProcessor extends Processor {

    /**
     * Interface from which a BeanProcessor can obtain another bean.
     */
    interface BeanCreator {
        /**
         * Obtains a new instance of the Bean this BeanProcessor handled. <br>
         * New instances have been processed by the same chain of BeanProcessors
         * that the original Bean had been. 
         * @return new instance of bean.
         */
        Object getBean();
    }    
    
    Object beforeInit(Object bean, String beanName, BeanCreator beanCreator, BeanMetadata beanData);

    Object afterInit(Object bean, String beanName, BeanCreator beanCreator, BeanMetadata beanData);

    void beforeDestroy(Object bean, String beanName);

    void afterDestroy(Object bean, String beanName);

}
