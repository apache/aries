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

import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NonNullMetadata;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.Target;

/**
 * A mutable version of the <code>ServiceMetadata</code> that allows modifications.
 *
 * @version $Rev$, $Date$
 */
public interface MutableServiceMetadata extends ServiceMetadata, MutableComponentMetadata {

    void setServiceComponent(Target serviceComponent);

    void addInterface(String interfaceName);

    void removeInterface(String interfaceName);

    void setAutoExport(int autoExportMode);

    void addServiceProperty(MapEntry serviceProperty);

    MapEntry addServiceProperty(NonNullMetadata key, Metadata value);

    void removeServiceProperty(MapEntry serviceProperty);

    void setRanking(int ranking);

    void addRegistrationListener(RegistrationListener listener);

    RegistrationListener addRegistrationListener(Target listenerComponent,
                                                 String registrationMethodName,
                                                 String unregistrationMethodName);

    void removeRegistrationListener(RegistrationListener listener);

}