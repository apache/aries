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
package org.apache.aries.application.impl;

import org.apache.aries.application.VersionRange;
import org.osgi.framework.Version;

public final class VersionRangeImpl implements VersionRange {

  private org.apache.aries.util.VersionRange versionRange;
  /**
   * 
   * @param version   version for the verioninfo
   */
  public VersionRangeImpl(String version) {
      versionRange = new org.apache.aries.util.VersionRange(version);
  }

  /**
   * 
   * @param version             version for the versioninfo
   * @param exactVersion        whether this is an exact version
   */
  public VersionRangeImpl(String version, boolean exactVersion) {
      versionRange = new org.apache.aries.util.VersionRange(version, exactVersion);
  }

  private VersionRangeImpl(org.apache.aries.util.VersionRange versionRange) {
      this.versionRange = versionRange;
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.aries.application.impl.VersionRange#toString()
   */
  @Override
  public String toString() {
      return versionRange.toString();
  }

  @Override
  public int hashCode() {
      return versionRange.hashCode();
  }
  
  @Override
  public boolean equals(Object other) {
      boolean result = false;
      if (this == other) {
          result = true;
      } else if (other instanceof VersionRangeImpl) {
          VersionRangeImpl vr = (VersionRangeImpl) other;   
          result = versionRange.equals(vr.versionRange);
      }
      return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.aries.application.impl.VersionRange#getExactVersion()
   */
  public Version getExactVersion() {
      return versionRange.getExactVersion();
  }

  /* (non-Javadoc)
   * @see org.apache.aries.application.impl.VersionRange#getMaximumVersion()
   */
  public Version getMaximumVersion() {
      return versionRange.getMaximumVersion();
  }

  /* (non-Javadoc)
   * @see org.apache.aries.application.impl.VersionRange#getMinimumVersion()
   */
  public Version getMinimumVersion() {
      return versionRange.getMinimumVersion();
  }

  /* (non-Javadoc)
   * @see org.apache.aries.application.impl.VersionRange#isMaximumExclusive()
   */
  public boolean isMaximumExclusive() {
      return versionRange.isMaximumExclusive();
  }

  /* (non-Javadoc)
   * @see org.apache.aries.application.impl.VersionRange#isMaximumUnbounded()
   */
  public boolean isMaximumUnbounded() {
      return versionRange.isMaximumUnbounded();
  }

  /* (non-Javadoc)
   * @see org.apache.aries.application.impl.VersionRange#isMinimumExclusive()
   */
  public boolean isMinimumExclusive() {
      return versionRange.isMinimumExclusive();
  }
  
  /**
   * This method checks that the provided version matches the desired version.
   * 
   * @param version
   *          the version.
   * @return true if the version matches, false otherwise.
   */
  public boolean matches(Version version) {
      return versionRange.matches(version);
  }

  /* (non-Javadoc)
   * @see org.apache.aries.application.impl.VersionRange#isExactVersion()
   */
  public boolean isExactVersion() {
      return versionRange.isExactVersion();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.aries.application.impl.VersionRange#intersect(VersionRange
   * range)
   */
  public VersionRange intersect(VersionRange r) {
      VersionRangeImpl rr = (VersionRangeImpl) r;      
      org.apache.aries.util.VersionRange result = versionRange.intersect(rr.versionRange);
      return (result == null) ? null : new VersionRangeImpl(result);
  }

}
