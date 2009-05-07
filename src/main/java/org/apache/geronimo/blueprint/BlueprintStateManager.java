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
package org.apache.geronimo.blueprint;

import org.osgi.framework.Bundle;

/**
 * An OSGi service implementing this interface will be registered in the OSGi registry.
 * It enables querying the blueprint status for a given bundle.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public interface BlueprintStateManager {

    /**
     * The bundle is not a blueprint bundle or has not been started yet.
     */
    int UNKNOWN = 0;

    /**
     * The blueprint context for the bundle is being created.
     */
    int CREATING = 1;

    /**
     * The bundle is waiting for mandatory service references.
     */
    int WAITING = 2;

    /**
     * The blueprint context has been succesfully created.
     */
    int CREATED = 3;

    /**
     * Creation of the blueprint context has failed.
     */
    int FAILED = 4;

    /**
     * The blueprint context for this bundle is being destroyed.
     */
    int DESTROYING = 5;

    /**
     * The blueprint context for this bundle has been destroyed.
     */
    int DESTROYED = 6;

    /**
     * Retrieve the blueprint state for the given bundle.
     *
     * @param bundle the bundle to check
     * @return the blueprint state
     */
    int getState(Bundle bundle);

    /**
     * Retrieve the cause of the failure for a given bundle.
     *
     * @param bundle the bundle to check
     * @return a <code>Throwable</code> or <code>null</code> if the bundle has not failed or if no failure cause was provided
     */
    Throwable getFailure(Bundle bundle);

}
