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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.quiesce.participant;

import java.util.List;
import org.apache.aries.quiesce.manager.QuiesceCallback;
import org.osgi.framework.Bundle;

/**
 * Interface for OSGi containers / extenders to hook into the quiesce mechanism. An extender such
 * as Blueprint should implement a {@link QuiesceParticipant} and register it as a service in the service 
 * registry.
 */
public interface QuiesceParticipant
{
  /**
   * Request a number of bundles to be quiesced by this participant
   * 
   * This method must be non-blocking.
   * @param callback The callback with which to alert the manager of successful quiesce completion (from the view of this
   * participant)
   * @param bundlesToQuiesce The bundles scheduled to be quiesced
   */
  public void quiesce(QuiesceCallback callback, List<Bundle> bundlesToQuiesce);
}
