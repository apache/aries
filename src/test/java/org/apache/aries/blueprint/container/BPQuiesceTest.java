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
package org.apache.aries.blueprint.container;

import static junit.framework.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.aries.quiesce.manager.QuiesceCallback;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class BPQuiesceTest {
  @Test
  public void canQuiesceNoBPBundle() throws Exception {
    IMocksControl c = EasyMock.createControl();
    BundleContext ctx = c.createMock(BundleContext.class);
    Bundle bpBundle = c.createMock(Bundle.class);
    Bundle testBundle = c.createMock(Bundle.class);
    
    EasyMock.expect(ctx.getBundle()).andReturn(bpBundle);
    
    BlueprintQuiesceParticipant bqp = new BlueprintQuiesceParticipant(ctx, new BlueprintExtender() {
      @Override
      protected BlueprintContainerImpl getBlueprintContainerImpl(Bundle bundle) {
        return null;
      }      
    });
    
    final Semaphore result = new Semaphore(0);
    
    QuiesceCallback qc = new QuiesceCallback() {
      public void bundleQuiesced(Bundle... bundlesQuiesced) {
        result.release();
      }
    };
    c.replay();
    bqp.quiesce(qc, Arrays.asList(testBundle));
    c.verify();
    assertTrue(result.tryAcquire(2, TimeUnit.SECONDS));
  }
}
