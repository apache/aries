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
package org.apache.geronimo.blueprint;

import java.security.AccessControlContext;
import java.util.Dictionary;
import java.util.List;

import org.apache.geronimo.blueprint.container.ServiceRecipe;
import org.apache.geronimo.blueprint.di.Repository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.osgi.service.blueprint.container.Converter;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 766508 $, $Date: 2009-04-19 22:09:27 +0200 (Sun, 19 Apr 2009) $
 */
public interface ExtendedBlueprintContainer extends BlueprintContainer {

    BundleContext getBundleContext();

    Bundle getExtenderBundle();

    BlueprintListener getEventDispatcher();

    Converter getConverter();

    Class loadClass(String name) throws ClassNotFoundException;

    ComponentDefinitionRegistry getComponentDefinitionRegistry();

    <T extends Processor> List<T> getProcessors(Class<T> type);

    Repository getRepository();
    
    ServiceRegistration registerService(String[] classes, Object service, Dictionary properties);
    
    Object getService(ServiceReference reference);
    
    AccessControlContext getAccessControlContext();
            
}
