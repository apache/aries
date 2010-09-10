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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.quiesce.manager.QuiesceCallback;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class BlueprintQuiesceParticipant implements QuiesceParticipant 
{
	private BundleContext ctx;
	private BlueprintExtender extender;
	
	public BlueprintQuiesceParticipant(BundleContext context, BlueprintExtender extender)
	{
		this.ctx = context;
		this.extender = extender;
	}

	/**
	 * A Threadpool for running quiesce operations
	 */
	private final ExecutorService executor = new ThreadPoolExecutor(0, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() 
	{
		public Thread newThread(Runnable r) 
		{
	        Thread t = new Thread(r, "Blueprint-Container-ThreadPool");
	        t.setDaemon(true);
	        return t;
		}
    });
	
	public void quiesce(QuiesceCallback callback, List<Bundle> bundlesToQuiesce) 
	{
		for(Bundle b : bundlesToQuiesce) 
		{
		  try 
		  {
		    executor.execute(new QuiesceBundle(callback, b, extender));
		  } 
		  catch (RejectedExecutionException re) {
		  }
		  
		  //If we are quiescing, then we need to quiesce this threadpool!
		  if(b.equals(ctx.getBundle()))
		    executor.shutdown();
		}
	}

  /**
   * A runnable Quiesce operation for a single bundle
   */
	private static final class QuiesceBundle implements Runnable 
	{
		/** The bundle being quiesced */
		private final Bundle bundleToQuiesce;
		private QuiesceCallback callback;
		private BlueprintExtender extender;
		
		public QuiesceBundle(QuiesceCallback callback, Bundle bundleToQuiesce, 
				BlueprintExtender extender) 
		{	
			super();
			this.callback = callback;
			this.bundleToQuiesce = bundleToQuiesce;
			this.extender = extender;
		}

		public void run() 
		{
			BlueprintContainerImpl container = extender.getBlueprintContainerImpl(bundleToQuiesce);
						
			BlueprintRepository repository = container.getRepository();
			Set<String> names = repository.getNames();
			container.quiesce();
			boolean hasServices = false;
			
			for (String name: names)
			{
				Recipe recipe = repository.getRecipe(name);
				if (recipe instanceof ServiceRecipe)
				{
					hasServices = true;
					((ServiceRecipe)recipe).quiesce(new QuiesceDelegatingCallback(callback, bundleToQuiesce));
				}
			}
			//If the bundle has no services we can quiesce immediately
			if (!hasServices)
			{
				callback.bundleQuiesced(bundleToQuiesce);
			}
		}
	}
	
    /**
	 * A wrapper to protect our internals from the Quiesce API so that we can make it
	 * an optional dependency
	 */
	private static final class QuiesceDelegatingCallback implements DestroyCallback 
	{
	  
	  /** The callback to delegate to */
	  private final QuiesceCallback callback;
	
	  /** The single bundle being quiesced by this DestroyCallback */
	  private final Bundle toQuiesce;
	
	  private Set<String> services = new HashSet<String>();
	    
	  public QuiesceDelegatingCallback(QuiesceCallback cbk, Bundle b) 
	  {
	    callback = cbk;
	    toQuiesce = b;
	      
	    ServiceReference[] serviceRefs = b.getRegisteredServices();
	    
	    for (ServiceReference ref : serviceRefs)
	    {
	  	  services.add(b.getBundleContext().getService(ref).toString());
	    }
	  }
	    
	  public void callback(Object key) 
	  {
	    if (key != null && services.remove(key.toString()) && services.isEmpty())
	    {
	 	  	callback.bundleQuiesced(toQuiesce);
	    }
	  }
	}
}
