/*
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
package org.osgi.service.blueprint.reflect;

import java.util.Collection;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Mar 26, 2009
 * Time: 11:45:26 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ServiceReferenceComponentMetadata extends ComponentMetadata {

    /**
     * A matching service is required at all times.
     */
    public static final int AVAILABILITY_MANDATORY = 1;

    /**
     * A matching service is not required to be present.
     */
    public static final int AVAILABILITY_OPTIONAL = 2;

    /**
     * Whether or not a matching service is required at all times.
     *
     * @return one of MANDATORY_AVAILABILITY or OPTIONAL_AVAILABILITY
     */
    int getServiceAvailabilitySpecification();

    /**
     * The interface types that the matching service must support
     *
     * @return an array of type names
     */
    Set getInterfaceNames();

    /**
     * The value of the component name attribute, if specified.
     *
     * @return the component name attribute value, or null if the attribute was not specified
     */
    String getComponentName();

    /**
     * The filter expression that a matching service must pass
     *
     * @return filter expression
     */
    String getFilter();

    /**
     * The set of listeners registered to receive bind and unbind events for
     * backing services.
     *
     * @return an immutable collection of registered BindingListenerMetadata
     */
    Collection getBindingListeners();

}
