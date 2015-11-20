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
package org.apache.aries.blueprint.spring;

import java.net.URL;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.felix.utils.extender.AbstractExtender;
import org.apache.felix.utils.extender.Extension;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring namespace extender.
 * This OSGi extender is responsible for registering spring namespaces for blueprint.
 *
 * @see SpringExtension
 */
public class Activator extends AbstractExtender {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    ServiceRegistration<NamespaceHandler> registration;

    @Override
    protected Extension doCreateExtension(Bundle bundle) throws Exception {
        URL handlers = bundle.getResource(SpringExtension.SPRING_HANDLERS);
        if (handlers != null) {
            return new SpringExtension(bundle);
        }
        return null;
    }

    @Override
    protected void debug(Bundle bundle, String msg) {
        LOGGER.debug(msg + ": " + bundle.getSymbolicName() + "/" + bundle.getVersion());
    }

    @Override
    protected void warn(Bundle bundle, String msg, Throwable t) {
        LOGGER.warn(msg + ": " + bundle.getSymbolicName() + "/" + bundle.getVersion(), t);
    }

    @Override
    protected void error(String msg, Throwable t) {
        LOGGER.error(msg, t);
    }

}
