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
    String PROVIDE_CAPABILITY = "Provide-Capability";
    String REQUIRE_CAPABILITY = "Require-Capability";

    String SPI_CONSUMER_HEADER = "SPI-Consumer";
    String SPI_PROVIDER_HEADER = "SPI-Provider";

    String SPI_CAPABILITY_NAMESPACE = "osgi.spi.provider";
    String EXTENDER_CAPABILITY_NAMESPACE = "osgi.jse.serviceloader";
    String BUNDLE_VERSION_ATTRIBUTE = "bundle-version";

    String PROCESSED_SPI_CONSUMER_HEADER = "X-SpiFly-Processed-SPI-Consumer";

    String SPI_PROVIDER_URL = "spi.provider.url";
}
