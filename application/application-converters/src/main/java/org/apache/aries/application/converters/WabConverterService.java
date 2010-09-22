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
package org.apache.aries.application.converters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.spi.convert.BundleConversion;
import org.apache.aries.application.management.spi.convert.BundleConverter;
import org.apache.aries.application.utils.management.SimpleBundleInfo;
import org.apache.aries.application.utils.manifest.BundleManifest;
import org.apache.aries.web.converter.WabConversion;
import org.apache.aries.web.converter.WarToWabConverter;
import org.apache.aries.web.converter.WarToWabConverter.InputStreamProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WabConverterService implements BundleConverter {
    private static final String WAR_FILE_EXTENSION = ".war";
    private static final Logger LOGGER = LoggerFactory.getLogger(WabConverterService.class);
    
    private WarToWabConverter wabConverter;

    public WarToWabConverter getWabConverter() {
        return wabConverter;
    }

    public void setWabConverter(WarToWabConverter wabConverter) {
        this.wabConverter = wabConverter;
    }

    public BundleConversion convert(IDirectory parentEba, final IFile toBeConverted) {
        if (toBeConverted.getName().endsWith(WAR_FILE_EXTENSION)) {
            try {
            	final WabConversion conversion = wabConverter.convert(new InputStreamProvider() {
                    public InputStream getInputStream() throws IOException {
                        return toBeConverted.open();
                    }
                }, toBeConverted.getName(), new Properties());
            	            	
                return new BundleConversion() {

					public BundleInfo getBundleInfo() throws IOException {
						return new SimpleBundleInfo(BundleManifest.fromBundle(conversion.getWAB()), toBeConverted.toString());
					}

					public InputStream getInputStream() throws IOException {
						return conversion.getWAB();
					}
                	
                };
            } catch (IOException e) {
                LOGGER.error("Encountered an exception while converting " + toBeConverted.getName() 
                        + " in " + parentEba.getName(), e);
            }
        }

        return null;
    }
}
