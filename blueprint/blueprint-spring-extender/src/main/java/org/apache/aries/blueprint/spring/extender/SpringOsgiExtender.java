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

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.aries.blueprint.services.BlueprintExtenderService;
import org.apache.aries.blueprint.utils.HeaderParser;
import org.apache.aries.blueprint.utils.HeaderParser.PathElement;
import org.apache.felix.utils.extender.AbstractExtender;
import org.apache.felix.utils.extender.Extension;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring namespace extender.
 * This OSGi extender is responsible for registering spring namespaces for blueprint.
 *
 * @see SpringOsgiExtension
 */
public class SpringOsgiExtender extends AbstractExtender {

    public static final String SPRING_CONTEXT_HEADER = "Spring-Context";

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringOsgiExtender.class);

    private final BlueprintExtenderService blueprintExtenderService;

    public SpringOsgiExtender(BlueprintExtenderService blueprintExtenderService) {
        this.blueprintExtenderService = blueprintExtenderService;
    }

    @Override
    protected Extension doCreateExtension(Bundle bundle) throws Exception {
        List<URL> paths = getSpringPaths(bundle);
        if (paths != null && !paths.isEmpty()) {
            return new SpringOsgiExtension(blueprintExtenderService, bundle, paths);
        }
        return null;
    }

    private List<URL> getSpringPaths(Bundle bundle) throws Exception {
        LOGGER.debug("Scanning bundle {}/{} for spring application", bundle.getSymbolicName(), bundle.getVersion());
        List<URL> pathList = new ArrayList<URL>();
        String springHeader = bundle.getHeaders().get(SPRING_CONTEXT_HEADER);
        if (springHeader == null) {
            springHeader = "*";
        }
        List<PathElement> paths = HeaderParser.parseHeader(springHeader);
        for (PathElement path : paths) {
            String name = path.getName();
            if ("*".equals(name)) {
                name = "META-INF/spring/*.xml";
            }
            String baseName;
            String filePattern;
            int pos = name.lastIndexOf('/');
            if (pos < 0) {
                baseName = "/";
                filePattern = name;
            } else {
                baseName = name.substring(0, pos + 1);
                filePattern = name.substring(pos + 1);
            }
            if (filePattern.contains("*")) {
                Enumeration<URL> e = bundle.findEntries(baseName, filePattern, false);
                while (e != null && e.hasMoreElements()) {
                    pathList.add(e.nextElement());
                }
            } else {
                pathList.add(bundle.getEntry(name));
            }
        }
        if (!pathList.isEmpty()) {
            LOGGER.debug("Found spring application in bundle {}/{} with paths: {}", bundle.getSymbolicName(), bundle.getVersion(), pathList);
            // Check compatibility
            // TODO: For lazy bundles, the class is either loaded from an imported package or not found, so it should
            // not trigger the activation.  If it does, we need to use something else like package admin or
            // ServiceReference, or just not do this check, which could be quite harmful.
            if (isCompatible(bundle)) {
                return pathList;
            } else {
                LOGGER.info("Bundle {}/{} is not compatible with this blueprint extender", bundle.getSymbolicName(), bundle.getVersion());
            }
        } else {
            LOGGER.debug("No blueprint application found in bundle {}/{}", bundle.getSymbolicName(), bundle.getVersion());
        }
        return null;
    }

    private boolean isCompatible(Bundle bundle) {
        return true;
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
