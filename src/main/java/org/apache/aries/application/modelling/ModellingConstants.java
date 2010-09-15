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
package org.apache.aries.application.modelling;

import org.osgi.framework.Constants;

public class ModellingConstants
{
  public static final String OBR_SYMBOLIC_NAME = "symbolicname";
  public static final String OBR_PRESENTATION_NAME = "presentationname";
  public static final String OBR_MANIFEST_VERSION = "manifestversion";
  public static final String OBR_BUNDLE = "bundle";
  public static final String OBR_PACKAGE = "package";
  public static final String OBR_SERVICE = "service";
  public static final String OBR_COMPOSITE_BUNDLE = "composite-bundle";
  public static final String OPTIONAL_KEY = Constants.RESOLUTION_DIRECTIVE + ":";
}
