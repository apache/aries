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
package org.apache.aries.quiesce.manager;

import java.util.List;
import java.util.concurrent.Future;

import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.osgi.framework.Bundle;

/**
 * Interface for the quiesce manager. A quiesce manager provides the functionality to stop
 * bundles in such a manner that currently running work can be safely finished. To exploit this
 * above the quiesce manager individual containers / extenders (such as blueprint, jpa etc) need to 
 * quiesce aware and register {@link QuiesceParticipant} appropriately.
 */
public interface QuiesceManager
{
  /** 
   * Request a collection of bundles to be quiesced
   * 
   * @param timeout time to wait (in milliseconds) for all the quiesce participants to finish 
   * before stopping the bundles. If some quiesce participants do not finish within the given timeout the bundles
   * are stopped regardless at the timeout
   * @param bundlesToQuiesce
   */
  public void quiesce(long timeout, List<Bundle> bundlesToQuiesce);
  
  /**
   * Request a collection of bundles to be quiesced using the default timeout
   * 
   * @param bundlesToQuiesce
   */
  public void quiesce(List<Bundle> bundlesToQuiesce);

  /**
   * Request a collection of bundles to be quiesced like <code>quiesce(long, List&lt;Bundle&gt;)</code>
   * return a {@link Future} that the caller can block on instead of void
   * 
   * @param bundlesToQuiesce
   * @return a {@link Future} that captures the execution of quiesce. The returned {@link Future} does
   * not support the cancel operation.
   */
  public Future<?> quiesceWithFuture(List<Bundle> bundlesToQuiesce);

  
  /**
   * Request a collection of bundles to be quiesced like <code>quiesce(long, List&lt;Bundle&gt;)</code>
   * return a {@link Future} that the caller can block on instead of void
   * 
   * @param timeout time to wait (in milliseconds) for all the quiesce participants to finish 
   * before stopping the bundles. If some quiesce participants do not finish within the given timeout the bundles
   * are stopped regardless at the timeout
   * @param bundlesToQuiesce
   * @return a {@link Future} that captures the execution of quiesce. The returned {@link Future} does
   * not support the cancel operation.
   */
  public Future<?> quiesceWithFuture(long timeout, List<Bundle> bundlesToQuiesce);
}