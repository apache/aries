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
package org.apache.aries.ejb.openejb.extender;

import java.io.IOException;
import java.net.URL;

import org.apache.xbean.finder.AbstractFinder;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSGiFinder extends AbstractFinder {

  private final Bundle b;

  private static final Logger logger = LoggerFactory.getLogger(OSGiFinder.class);
  
  public OSGiFinder(Bundle bundle) {
    b = bundle;
    
    for(Object r : ((BundleWiring) bundle.adapt(BundleWiring.class)).listResources("/", "*.class", BundleWiring.LISTRESOURCES_RECURSE)) {
        String resource = (String) r;
      try {
        readClassDef(getResource(resource).openStream());
      } catch (IOException e) {
        logger.warn("Error processing class file " + resource);
      }
    }
  }

  @Override
  protected URL getResource(String name) {
    return b.getResource(name);
  }

  @Override
  protected Class<?> loadClass(String className) throws ClassNotFoundException {
    return b.loadClass(className);
  }
}
