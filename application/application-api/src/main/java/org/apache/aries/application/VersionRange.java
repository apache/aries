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
package org.apache.aries.application;

import org.osgi.framework.Version;

/**
 * A representation of a Version Range. @see <a href="http://www.osgi.org/Release4/HomePage">
 * section 3.2.6</a> of the OSGi Service Platform Core Specification. 
 */
public interface VersionRange
{
  /**
   * this method returns the exact version from the versionInfo obj.
   * this is used for DeploymentContent only to return a valid exact version
   * otherwise, null is returned.
   * @return
   */
  public abstract Version getExactVersion();

  /**
   * get the maximum version
   * @return    the maximum version
   */
  public abstract Version getMaximumVersion();

  /**
   * get the minimum version
   * @return    the minimum version
   */
  public abstract Version getMinimumVersion();

  /**
   * is the maximum version exclusive
   * @return  
   */
  public abstract boolean isMaximumExclusive();

  /**
   * is the maximum version unbounded
   * @return
   */
  public abstract boolean isMaximumUnbounded();

  /**
   * is the minimum version exclusive
   * @return
   */
  public abstract boolean isMinimumExclusive();

  /**
   * check if the versioninfo is the exact version
   * @return
   */
  public abstract boolean isExactVersion();
  /**
   * This method tests to see if the provided version is inside this range.
   * 
   * @param version the version to test.
   * @return        true if the version matches, false otherwise.
   */
  public boolean matches(Version version);
}