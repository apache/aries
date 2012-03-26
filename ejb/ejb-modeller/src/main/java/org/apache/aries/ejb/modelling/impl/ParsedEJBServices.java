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
package org.apache.aries.ejb.modelling.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ParsedServiceElements;
import org.apache.aries.ejb.modelling.EJBRegistry;

/**
 * An {@link EJBRegistry} that marks the {@link ParsedServiceElements} provided
 * by the EJB bundle.
 * @author Tim
 *
 */
public class ParsedEJBServices implements ParsedServiceElements, EJBRegistry {

  private final Collection<ImportedService> references;
  private final Collection<ExportedService> services;
  
  private boolean all;
  private Set<String> allowedNames; 
  
  public ParsedEJBServices() {
    this.references = Collections.emptyList();
    this.services = new ArrayList<ExportedService>();
    allowedNames = new HashSet<String>();
    all = false;
  }

  public Collection<ImportedService> getReferences() {
    return references;
  }

  public Collection<ExportedService> getServices() {
    return Collections.unmodifiableCollection(services);
  }
  
  public void setAllowedNames(Collection<String> names) {
    if(names.contains("NONE")) {
      all= false;
      allowedNames.clear();
      return;
    }

    if(names.size() == 1 && "".equals(names.iterator().next())) {
        all = true;
        return;
    }
    
    allowedNames.addAll(names);
  }
  
  public void addEJBView(String ejbName, String ejbType, String interfaceName,
      boolean remote) {
    if(ejbType.equalsIgnoreCase("Stateful"))
      return;
    
    if(all || allowedNames.contains(ejbName))
      services.add(new EJBServiceExport(ejbName, ejbType, interfaceName, remote));
  }
}
