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

package org.apache.aries.application.management.impl;

/**
 * An administrator may supply an application.eba file that will be changed before deployment.  
 * We may augment, or even create, META-INF/APPLICATION.MF. 
 * We will create META-INF/DEPLOYMENT.MF if one does not exist. 
 * We may migrate plain .war files to WABs, and may also migrate plain .jar files to become 
 * well formed bundles. Thus the .eba file that will be subsequently used to provide bundles into 
 * the application server's runtime may differ from that originally provided by the administrator. 
 * This class handles the creation of the modified .eba artifact. 
 */
public class ExplodedEBA {

  // TODO
  
}
