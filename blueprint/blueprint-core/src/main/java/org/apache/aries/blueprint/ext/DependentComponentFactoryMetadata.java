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
package org.apache.aries.blueprint.ext;

/**
 * Metadata for custom components that need to plug in to the
 * Blueprint container lifecycle for beans
 */
public interface DependentComponentFactoryMetadata extends ComponentFactoryMetadata {
    /**
     * Interface that allows to notify the container when the dependencies of the component
     * become satisfied or unsatified.
     */
    interface SatisfactionCallback {
        /**
         * Alert the container that the satisfaction status has changed. isSatisfied() should reflect this.
         */
        void notifyChanged();
    }
    
    /**
     * Start tracking the dependencies for this component.
     * @param observer The container callback for alerting the container of status changes
     */
    void startTracking(SatisfactionCallback observer);
    
    /**
     * Stop tracking the dependencies for this component.
     */
    void stopTracking();
    
    /**
     * Return a string representation of the dependencies of this component. This will be used
     * in diagnostics as well as the GRACE_PERIOD event.
     * @return
     */
    String getDependencyDescriptor();

    /**
     * Are all dependencies of this component satisfied?
     * @return
     */
    boolean isSatisfied();
}
