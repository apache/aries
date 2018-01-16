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
package org.apache.aries.blueprint.spring.extender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.SpringVersion;

/**
 * Resolves spring xsd version.
 *
 */
public class SpringXsdVersionResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringXsdVersionResolver.class);
    private static final String DEFAULT_VERSION = "4.3"; // latest version
    private static final Pattern PATTERN = Pattern.compile("^\\d*\\.\\d*");

    /**
     * It will call a Spring method that returns the full version. Expects
     * format 4.2.2.RELEASE and 4.2 will be returned.
     *
     * @return String containing the xsd version.
     */
    public static String resolve() {
        final String fullVersion = SpringVersion.getVersion();
        if (fullVersion != null) {
            final Matcher matcher = PATTERN.matcher(fullVersion);
            if (matcher.find()) {
                return matcher.group(0);
            }
        }
        LOGGER.trace("Could not resolve xsd version from Spring's version {}", fullVersion);

        return DEFAULT_VERSION;

    }
}
