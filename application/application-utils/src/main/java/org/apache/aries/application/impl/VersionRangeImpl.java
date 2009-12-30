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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Version;

import org.apache.aries.application.VersionRange;
import org.apache.aries.application.utils.internal.MessageUtil;

public final class VersionRangeImpl implements VersionRange
{
  private String version;
  /** The minimum desired version for the bundle */
  private Version minimumVersion;
  /** The maximum desired version for the bundle */
  private Version maximumVersion;
  /** True if the match is exclusive of the minimum version */
  private boolean minimumExclusive;
  /** True if the match is exclusive of the maximum version */
  private boolean maximumExclusive;
  /** A regexp to select the version */
  private static final Pattern versionCapture = Pattern.compile("\"?(.*?)\"?$");
  
  /**
   * 
   * @param version   version for the verioninfo
   */
  public VersionRangeImpl(String version) {
    this.version = version;
    processVersionAttribute(this.version);
  }
  
  /**
   * 
   * @param version             version for the verioninfo
   * @param exactVersion        whether this is an exact version
   */
  public VersionRangeImpl(String version, boolean exactVersion) {;
    this.version = version;
    if (exactVersion) {
      processExactVersionAttribute(this.version);
    } else {
      processVersionAttribute(this.version);
    }
  }
  
  /* (non-Javadoc)
   * @see org.apache.aries.application.impl.VersionRange#toString()
   */
  @Override
  public String toString() {
    return this.version;
  }
  
  public int hashCode()
  {
    return version.hashCode();
  }
  
  public boolean equals(Object other)
  {
    if (other == this) return true;
    if (other == null) return false;
    
    if (other instanceof VersionRangeImpl) {
      return version.equals(((VersionRangeImpl)other).version);
    }
    
    return false;
  }
  
  /* (non-Javadoc)
 * @see org.apache.aries.application.impl.VersionRange#getExactVersion()
 */
  public Version getExactVersion() {
    Version v = null;
    if (isExactVersion()) {
      v = getMinimumVersion();
    } 
    return v;
  }
  
  /* (non-Javadoc)
 * @see org.apache.aries.application.impl.VersionRange#getMaximumVersion()
 */
  public Version getMaximumVersion()
  {
    return maximumVersion;
  }

  /* (non-Javadoc)
 * @see org.apache.aries.application.impl.VersionRange#getMinimumVersion()
 */
  public Version getMinimumVersion()
  {
    return minimumVersion;
  }

  /* (non-Javadoc)
 * @see org.apache.aries.application.impl.VersionRange#isMaximumExclusive()
 */
  public boolean isMaximumExclusive()
  {
    return maximumExclusive;
  }

  /* (non-Javadoc)
 * @see org.apache.aries.application.impl.VersionRange#isMaximumUnbounded()
 */
  public boolean isMaximumUnbounded()
  {
    boolean unbounded = maximumVersion == null;
    return unbounded;
  }

  /* (non-Javadoc)
 * @see org.apache.aries.application.impl.VersionRange#isMinimumExclusive()
 */
  public boolean isMinimumExclusive()
  {
    return minimumExclusive;
  }
  
  /**
   * this is designed for deployed-version as that is the exact version.
   * @param version
   * @return
   * @throws IllegalArgumentException
   */
  private boolean processExactVersionAttribute(String version) throws IllegalArgumentException{
    boolean success = processVersionAttribute(version);
    
    if (maximumVersion == null) {
      maximumVersion = minimumVersion;
    }

    if (!minimumVersion.equals(maximumVersion)) {
      throw new IllegalArgumentException(MessageUtil.getMessage("APPUTILS0011E", version));
    }

    if (!!!isExactVersion()) {
      throw new IllegalArgumentException(MessageUtil.getMessage("APPUTILS0009E", version));
    }

    return success;
  }
  /**
   * process the version attribute, 
   * @param version  the value to be processed
   * @return
   * @throws IllegalArgumentException
   */
  private boolean processVersionAttribute(String version) throws IllegalArgumentException{
    boolean success = false;
   
    Matcher matches = versionCapture.matcher(version);
    
    if (matches.matches()) {
      String versions = matches.group(1);
      
      if ((versions.startsWith("[") || versions.startsWith("(")) &&
          (versions.endsWith("]") || versions.endsWith(")"))) {
        if (versions.startsWith("[")) minimumExclusive = false;
        else if (versions.startsWith("(")) minimumExclusive = true;
        
        if (versions.endsWith("]")) maximumExclusive = false;
        else if (versions.endsWith(")")) maximumExclusive = true;
        
        int index = versions.indexOf(',');
        String minVersion = versions.substring(1, index);
        String maxVersion = versions.substring(index + 1, versions.length() - 1);
        
        try {
          minimumVersion = new Version(minVersion.trim());
          maximumVersion = new Version(maxVersion.trim());
          success = true;
        } catch (NumberFormatException nfe) {
          throw new IllegalArgumentException(MessageUtil.getMessage("APPUTILS0009E", version), nfe);
        }
      } else {
        try {
          if (versions.trim().length() == 0) minimumVersion = new Version(0,0,0);
          else minimumVersion = new Version(versions.trim());
          success = true;
        } catch (NumberFormatException nfe) {
          throw new IllegalArgumentException(MessageUtil.getMessage("APPUTILS0009E", version), nfe);
        }
      }      
    } else {
      throw new IllegalArgumentException(MessageUtil.getMessage("APPUTILS0009E", version));
    }
    
    return success;
  }

  public boolean matches(Version version)
  {
    boolean result;
    if (this.getMaximumVersion() == null) {
      result = this.getMinimumVersion().compareTo(version) <= 0;
    } else {
      int minN = this.isMinimumExclusive() ? 0 : 1;
      int maxN = this.isMaximumExclusive() ? 0 : 1;
      
      result = (this.getMinimumVersion().compareTo(version) < minN) &&
               (version.compareTo(this.getMaximumVersion()) < maxN);
    }
    return result;
  }

  /* (non-Javadoc)
 * @see org.apache.aries.application.impl.VersionRange#isExactVersion()
 */
  public boolean isExactVersion() {
    return minimumVersion.equals(maximumVersion) && minimumExclusive == maximumExclusive && !!!minimumExclusive;
  }
}
