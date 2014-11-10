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
package org.apache.aries.jpa.container.quiesce.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.aries.jpa.container.impl.PersistenceBundleManager;
import org.apache.aries.quiesce.manager.QuiesceCallback;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.apache.aries.util.AriesFrameworkUtil;
import org.apache.aries.util.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * This class provides Quiesce Participant support for JPA managed units. It is the only
 * class in this bundle that depends on the Quiesce API to make sure that the bundle can
 * optionally depend on the API. If no Quiesce API is available then this class will not be
 * loaded and no Quiesce support will be available.
 */
public class QuiesceParticipantImpl implements QuiesceParticipant, Closeable {
  
  /**
   * A wrapper to protect our internals from the Quiesce API so that we can make it
   * an optional dependency
   */
  private static final class QuiesceDelegatingCallback implements DestroyCallback {
    
    /** The callback to delegate to */
    private final QuiesceCallback callback;
    
    /** The single bundle being quiesced by this DestroyCallback */
    private final Bundle toQuiesce;

    public QuiesceDelegatingCallback(QuiesceCallback cbk, Bundle b) {
      callback = cbk;
      toQuiesce = b;
    }
    
    public void callback() {
      callback.bundleQuiesced(toQuiesce);
    }
    
  }
  
  /**
   * A runnable Quiesce operation for a single bundle
   */
  private static final class QuiesceBundle implements Runnable {
    
    /** The callback when we're done */
    private final DestroyCallback callback;
    /** The bundle being quiesced */
    private final Bundle bundleToQuiesce;
    /** The {@link PersistenceBundleManager} instance */
    private final QuiesceHandler quiesceHandler; 

    public QuiesceBundle(QuiesceCallback callback, Bundle bundleToQuiesce,
        QuiesceHandler quiesceHandler) {
      super();
      this.callback = new QuiesceDelegatingCallback(callback, bundleToQuiesce);
      this.bundleToQuiesce = bundleToQuiesce;
      this.quiesceHandler = quiesceHandler;
    }

    public void run() {
      quiesceHandler.quiesceBundle(bundleToQuiesce, callback);
    }
  }

  private static final int QUIESCABLE_BUNDLE = Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING;
  
  /**
   * A Threadpool for running quiesce operations
   */
  private final ExecutorService executor = new ThreadPoolExecutor(0, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
    
    public Thread newThread(Runnable r) {
      Thread t = new Thread(r, "JPA-Container-ThreadPool");
      t.setDaemon(true);
      return t;
    }
  });

  private QuiesceHandler quiesceHandler;
  private final BundleContext context;
  private ServiceRegistration quiesceReg;
  
  /** Some events that we need to tidy up */
  private final BlockingQueue<DestroyCallback> unhandledQuiesces = new LinkedBlockingQueue<DestroyCallback>();
  
  public QuiesceParticipantImpl(BundleContext context, QuiesceHandler quiesceHandler) {
    this.context = context;
    this.quiesceHandler = quiesceHandler;
    context.registerService(QuiesceParticipant.class.getName(), this, null);
  }


  public void quiesce(QuiesceCallback qc, List<Bundle> arg1) {
    boolean closeSelf = false;
    //Run a quiesce operation for each bundle being quiesced
    for(Bundle b : arg1) {
      try {
        if((b.getState() & QUIESCABLE_BUNDLE) == 0) {
          //This bundle is not in an "ACTIVE" state (or starting or stopping)
          qc.bundleQuiesced(b);
        } else {
          executor.execute(new QuiesceBundle(qc, b, quiesceHandler));
        }
      } catch (RejectedExecutionException re) {
        unhandledQuiesces.add(new QuiesceDelegatingCallback(qc, b));
      }
      //If we are quiescing, then we need to quiesce this threadpool!
      if(b.equals(context.getBundle())) {
          closeSelf = true;
      }
    }
    if (closeSelf) {
        IOUtils.close(this);
    }
  }
  
  @Override
  public void close() throws IOException {
      executor.shutdown();
      try {
        for(DestroyCallback cbk : unhandledQuiesces) {
          cbk.callback();
        }
        executor.awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        //We don't care
      }
      executor.shutdownNow();
      AriesFrameworkUtil.safeUnregisterService(quiesceReg);
  }
}
