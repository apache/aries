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

package org.apache.aries.application.install;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.application.management.ApplicationContext;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EBAInstaller implements ArtifactInstaller
{

  private static final Logger LOGGER = LoggerFactory.getLogger(EBAInstaller.class);

  private Map<File, ApplicationContext> appContexts = new HashMap<File, ApplicationContext>();

  private AriesApplicationManager applicationManager;

  public AriesApplicationManager getApplicationManager()
  {
    return applicationManager;
  }

  public void setApplicationManager(AriesApplicationManager applicationManager)
  {
    this.applicationManager = applicationManager;
  }

  public boolean canHandle(File fileToHandlerLocation)
  {
    return fileToHandlerLocation.getName().toLowerCase().endsWith(".eba");
  }

  public void install(File applicationLocation) throws Exception
  {
    AriesApplication app = applicationManager
        .createApplication(applicationLocation.toURI().toURL());
    
    String appSymName = app.getApplicationMetadata().getApplicationSymbolicName();
    Version appVersion = app.getApplicationMetadata().getApplicationVersion();

    LOGGER.debug("created app from {} : {} {} with contents {}", new Object[] {
        applicationLocation.getName(), appSymName, appVersion,
        app.getApplicationMetadata().getApplicationContents() });

    ApplicationContext context = applicationManager.install(app);

    LOGGER.debug("installed app {} {} state: {}", new Object[] {
        appSymName, appVersion,
        context.getApplicationState() });
    
    context.start();

    LOGGER.debug("started app {} {} state: {}", new Object[] {
        appSymName, appVersion,
        context.getApplicationState() });
    
    // Store the application context away because it is the application context we need
    // to pass to the application manager if we're later asked to uninstall the application
    appContexts.put(applicationLocation, context);
  }

  public void uninstall(File applicationLocation) throws Exception
  {
    ApplicationContext context = appContexts.get(applicationLocation);
    
    String appSymName = context.getApplication().getApplicationMetadata().getApplicationSymbolicName();
    Version appVersion = context.getApplication().getApplicationMetadata().getApplicationVersion();

    LOGGER.debug("uninstalling {} {} ", new Object[] {
        appSymName, appVersion });

    if (context != null) {
      context.stop();
      applicationManager.uninstall(context);
    }

    appContexts.remove(applicationLocation);
    
    LOGGER.debug("uninstalled {} {} state: {}", new Object[] {
        appSymName, appVersion,
        context.getApplicationState() });
  }

  public void update(File arg0) throws Exception
  {
    throw new UnsupportedOperationException("Updating .eba file is not supported");
  }
}
