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

    /*
     * Header names
     */

    String SPI_CONSUMER_HEADER = "SPI-Consumer";

    String SPI_PROVIDER_HEADER = "SPI-Provider";

    /*
     * Attributes to be used with the SPI-Provider and SPI-Consumer headers
     */

    String PROVIDER_NAME_ATTRIBUTE = "provider-name";

    String SERVICE_IDS_ATTRIBUTE = "service-ids";

    /*
     * Attributes to be used with services created using the 'old' approach
     */

    String SPI_PROVIDER_URL = "spi.provider.url";

    /*
     * Attributes to be used with services created using the 'new' approach
     */

    String SERVICE_ID_SERVICE_ATTRIBUTE = "ServiceId";

    String PROVIDER_NAME_SERVICE_ATTRIBUTE = "ProviderName";

    String API_NAME_SERVICE_ATTRIBUTE = "ApiName";

}
