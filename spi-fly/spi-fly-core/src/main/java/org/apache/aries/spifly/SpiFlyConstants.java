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
package org.apache.aries.spifly;

import org.osgi.framework.Version;

public interface SpiFlyConstants {
    String SPECIFICATION_VERSION_STRING = "1.0.0";
    Version SPECIFICATION_VERSION = new Version(SPECIFICATION_VERSION_STRING);
    // Not taken from OSGi Constants because this code needs to compile with the 4.2 OSGi classes.
    String PROVIDE_CAPABILITY = "Provide-Capability";
    String REQUIRE_CAPABILITY = "Require-Capability";
    String EXTENDER_CAPABILITY_NAMESPACE = "osgi.extender";
    String FILTER_DIRECTIVE = "filter:";

    // These are two proprietary headers which predated the ServiceLoader Mediator
    // specification and are more powerful than what is specified there
    String SPI_CONSUMER_HEADER = "SPI-Consumer";
    String SPI_PROVIDER_HEADER = "SPI-Provider";

    // ServiceLoader capability and related directive
    String SERVICELOADER_CAPABILITY_NAMESPACE = "osgi.serviceloader";
    String REGISTER_DIRECTIVE = "register:";

    // Service registration property
    String SERVICELOADER_MEDIATOR_PROPERTY = "serviceloader.mediator";
    String PROVIDER_IMPLCLASS_PROPERTY = ".org.apache.aries.spifly.provider.implclass";

    // The names of the extenders involved
    String PROCESSOR_EXTENDER_NAME = "osgi.serviceloader.processor";
    String REGISTRAR_EXTENDER_NAME = "osgi.serviceloader.registrar";

    // Pre-baked requirements for consumer and provider
    String CLIENT_REQUIREMENT = EXTENDER_CAPABILITY_NAMESPACE + "; " + FILTER_DIRECTIVE +
            "=\"(" + EXTENDER_CAPABILITY_NAMESPACE + "=" + PROCESSOR_EXTENDER_NAME + ")\"";
    String PROVIDER_REQUIREMENT = EXTENDER_CAPABILITY_NAMESPACE + "; " + FILTER_DIRECTIVE +
            "=\"(" + EXTENDER_CAPABILITY_NAMESPACE + "=" + REGISTRAR_EXTENDER_NAME + ")\"";

    String PROCESSED_SPI_CONSUMER_HEADER = "X-SpiFly-Processed-SPI-Consumer";
}
