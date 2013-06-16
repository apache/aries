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
package org.apache.aries.blueprint.mutable;

import org.apache.aries.blueprint.ExtendedServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListener;
import org.osgi.service.blueprint.reflect.Target;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.reflect.ValueMetadata;

/**
 * A mutable version of the <code>ServiceReferenceMetadata</code> that allows modifications.
 *
 * @version $Rev$, $Date$
 */
public interface MutableServiceReferenceMetadata extends ExtendedServiceReferenceMetadata, MutableComponentMetadata {

    void setAvailability(int availability);

    void setInterface(String interfaceName);

    void setComponentName(String componentName);

    void addServiceListener(ReferenceListener listener);

    ReferenceListener addServiceListener(Target listenerComponent,
                                String bindMethodName,
                                String unbindMethodName);

    void removeReferenceListener(ReferenceListener listener);

    void setProxyMethod(int proxyMethod);

    void setFilter(String filter);

    void setRuntimeInterface(Class clazz);
    
    /**
     * Used to set a {@link BundleContext} for this reference lookup. If this
     * is set to null (or left unset) then the bundle context of the blueprint
     * bundle will be used (normal behaviour)
     * @param bc
     */
    void setBundleContext(BundleContext bc);

    void setExtendedFilter(ValueMetadata filter);
}
