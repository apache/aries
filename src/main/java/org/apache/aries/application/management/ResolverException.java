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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.application.management;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.application.modelling.ModellingConstants;

/**
 * Exception thrown by the AriesApplicationResolver interface
 */
public class ResolverException extends Exception {

  private static final long serialVersionUID = 8176120468577397671L;
  private Map<String, String> _unsatisfiedRequirementMessages = new HashMap<String, String>();
  
  /**
   * Construct from an Exception
   * @param e
   */
  public ResolverException (Exception e) { 
    super(e);
  }
  /**
   * Construct from a String
   * @param s
   */
  public ResolverException (String s) { 
    super(s);
  }
  
  public void setUnsatisfiedRequirements (List<String> reqts) { 
    // Assume the type is unknown if we don't get told it
    for (String reqt : reqts)
    {
      _unsatisfiedRequirementMessages.put(reqt, ModellingConstants.OBR_UNKNOWN);
    }
  }
  public List<String> getUnsatisfiedRequirements() { 
    return new ArrayList<String>(_unsatisfiedRequirementMessages.keySet());
  }

  public void setUnsatisfiedRequirementsAndReasons(
      Map<String, String> unsatisfiedRequirements) {
    _unsatisfiedRequirementMessages = unsatisfiedRequirements;
  }
  
  public Map<String, String> getUnsatisfiedRequirementsAndReasons() {
      return _unsatisfiedRequirementMessages;
  }
}
