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

import org.osgi.service.blueprint.reflect.BeanMetadata;

/**
 * An extended <code>BeanMetadata</code> that allows specifying if
 * the bean instances are processors or not.
 * 
 * Such processors have to be instantiated before instantiating all
 * other singletons, but to avoid breaking the lazy activation of
 * bundles, the Blueprint container needs to be aware of those and not
 * try to load the class to perform some introspection.
 */
public interface ExtendedBeanMetadata extends BeanMetadata {

    boolean isProcessor();
    
    /**
     * Provide an actual class, this overrides the class name if set. This is
     * useful for Namespace Handler services that do not want to force the
     * Blueprint bundle to import implementation classes.
     *
     * @return Return the class to use in runtime or <code>null</code>.
     */
    
    Class<?> getRuntimeClass();

    /**
     * Whether the bean allows properties to be injected directly into its fields in the case
     * where an appropriate setter method is not available.
     * @return Whether field injection is allowed
     */
    boolean getFieldInjection();

    /**
     * Whether arguments / properties conversion is strict or lenient.
     * @return The type of conversion.
     */
    boolean getRawConversion();
}
