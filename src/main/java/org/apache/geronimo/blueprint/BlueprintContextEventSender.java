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

import org.osgi.service.blueprint.context.BlueprintContext;

/**
 * Interface used to send events related to blueprint context life cycle.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public interface BlueprintContextEventSender extends Destroyable {

    void sendCreating(BlueprintContext blueprintContext);
    void sendCreated(BlueprintContext blueprintContext);
    void sendDestroying(BlueprintContext blueprintContext);
    void sendDestroyed(BlueprintContext blueprintContext);
    void sendWaiting(BlueprintContext blueprintContext, String[] serviceObjectClass, String serviceFilter);
    void sendFailure(BlueprintContext blueprintContext, Throwable cause);
    void sendFailure(BlueprintContext blueprintContext, Throwable cause, String[] serviceObjectClass, String serviceFilter);

}
