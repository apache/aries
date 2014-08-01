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
package org.apache.aries.jpa.container.impl;

import org.osgi.framework.Version;
/**
 * This exception is thrown when a persistence descriptor contains
 * an set of version ranges with no common overlap
 */
public class InvalidRangeCombination extends Exception {

  /**
   * For Serialization
   */
  private static final long serialVersionUID = 3631484834936016561L;

  public InvalidRangeCombination(Version minVersion, boolean minExclusive,
      Version maxVersion, boolean maxExclusive) {
    super(NLS.MESSAGES.getMessage("no.overlap.for.version.range", getVersionRangeString(minVersion, minExclusive, maxVersion, maxExclusive)));
  }

  private static String getVersionRangeString(Version minVersion,
      boolean minExclusive, Version maxVersion, boolean maxExclusive) {
    
    if(maxVersion == null)
      return minVersion.toString();
    else
    return ((minExclusive) ? "(" : "[") + minVersion + "," + maxVersion + ((maxExclusive) ? ")" : "]");
  }
}
