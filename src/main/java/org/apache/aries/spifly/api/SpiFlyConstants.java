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
package org.apache.aries.spifly.api;

public interface SpiFlyConstants {
    // Not taken from OSGi Constants because this code needs to compile with the 4.2 OSGi classes.
    String REQUIRE_CAPABILITY = "Require-Capability";

    String SPI_CONSUMER_HEADER = "SPI-Consumer";
    String SPI_PROVIDER_HEADER = "SPI-Provider";

    String EXTENDER_CAPABILITY_NAMESPACE = "osgi.extender";

    String CLIENT_EXTENDER_NAME = "spi.consumer.mediator";
    String PROVIDER_EXTENDER_NAME = "spi.provider.mediator";

    String CLIENT_REQUIREMENT = EXTENDER_CAPABILITY_NAMESPACE + "; " + EXTENDER_CAPABILITY_NAMESPACE + "=" + CLIENT_EXTENDER_NAME;
    String PROVIDER_REQUIREMENT = EXTENDER_CAPABILITY_NAMESPACE + "; " + EXTENDER_CAPABILITY_NAMESPACE + "=" + PROVIDER_EXTENDER_NAME;

    String CONSUMED_SPI_CONDITION = "spi";

    String PROVIDED_SPI_DIRECTIVE = "provided-spi";
    String PROVIDER_FILTER_DIRECTIVE = "provider-filter";
    String SERVICE_REGISTRY_DIRECTIVE = "service-registry";

    String PROCESSED_SPI_CONSUMER_HEADER = "X-SpiFly-Processed-SPI-Consumer";

    String SPI_PROVIDER_URL_PROPERTY = "spi.provider.url";
}
