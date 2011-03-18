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
package org.apache.aries.blueprint.sample;

public class DestroyTest
{
  private Runnable target;
  private Exception destroyFailure;
  private boolean destroyed;
  
  public void setTarget(Runnable r)
  {
    target = r;
  }
  
  public Exception getDestroyFailure()
  {
    return destroyFailure;
  }
  
  public void destroy()
  {
    try {
      target.run();
    } catch (Exception e) {
      destroyFailure = e;
    }
    
    synchronized (this) {
      destroyed = true;
      notifyAll();
    }
  }

  public synchronized boolean waitForDestruction(int timeout)
  {
    long startTime = System.currentTimeMillis();
    
    while (!!!destroyed && System.currentTimeMillis() - startTime < timeout) {
      try {
        wait(100);
      } catch (InterruptedException e) {
      }
    }
    
    return destroyed;
  }
}