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
package org.apache.felix.blueprint;

import org.apache.felix.blueprint.context.ModuleContextImpl;
import org.osgi.service.blueprint.context.ModuleContext;
import org.osgi.service.blueprint.context.ModuleContextEventConstants;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Apr 13, 2009
 * Time: 11:18:06 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ModuleContextEventSender extends ModuleContextEventConstants {

    void sendCreating(ModuleContext moduleContext);
    void sendCreated(ModuleContext moduleContext);
    void sendDestroying(ModuleContext moduleContext);
    void sendDestroyed(ModuleContext moduleContext);
    void sendWaiting(ModuleContext moduleContext, String[] serviceObjectClass, String serviceFilter);
    void sendFailure(ModuleContext moduleContext, Throwable cause);
    void sendFailure(ModuleContext moduleContext, Throwable cause, String[] serviceObjectClass, String serviceFilter);

    void destroy();

}
