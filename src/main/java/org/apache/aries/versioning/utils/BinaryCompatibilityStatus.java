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
package org.apache.aries.versioning.utils;

public class BinaryCompatibilityStatus
{
  private final boolean compatible;
  private final String reason;
  
  public BinaryCompatibilityStatus(boolean compatible, String reason) {
    this.compatible = compatible;
    this.reason = reason;
  }

  public boolean isCompatible()
  {
    return compatible;
  }

  public String getReason()
  {
    return reason;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + (compatible ? 1231 : 1237);
    result = prime * result + ((reason == null) ? 0 : reason.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    BinaryCompatibilityStatus other = (BinaryCompatibilityStatus) obj;
    if (compatible != other.compatible) return false;
    if (reason == null) {
      if (other.reason != null) return false;
    } else if (!reason.equals(other.reason)) return false;
    return true;
  }
  

}
