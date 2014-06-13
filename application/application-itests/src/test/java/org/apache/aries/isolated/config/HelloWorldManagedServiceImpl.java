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
package org.apache.aries.isolated.config;

import java.util.Dictionary;
import java.util.Properties;

import org.apache.aries.isolated.sample.HelloWorld;
import org.apache.aries.isolated.sample.HelloWorldImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class HelloWorldManagedServiceImpl implements BundleActivator, ManagedService
{
	private BundleContext context;
	private HelloWorldImpl hw;
	private ServiceRegistration msRegistration;
	private ServiceRegistration hwRegistration;
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception
	{
		Properties props = new Properties();
		props.put(Constants.SERVICE_PID, "helloworld-mn");
		this.context = context;
		this.msRegistration = context.registerService(ManagedService.class.getName(), this, (Dictionary) props);
		this.hwRegistration = null;
		
		//manually call our update to make sure the HW service is exposed out
		updated(null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public synchronized void stop(BundleContext context) throws Exception 
	{
		this.msRegistration.unregister();
		
		if (this.hwRegistration != null)
		{
			this.hwRegistration.unregister();
		}
		
		this.context = null;
		this.hwRegistration = null;
		this.msRegistration = null;
	}

	/**
	 * This method will re-register the helloworld service to easily track when updates
	 * occur to configuration.
	 */
	public synchronized void updated(Dictionary properties) throws ConfigurationException 
	{
		if (context != null) //means we have been stopped
		{
			if (hwRegistration != null)
			{
				hwRegistration.unregister();
			}
			
			if (hw == null)
			{
				hw = new HelloWorldImpl();
			}
			
			if (properties != null)
			{
				hw.setMessage((String)properties.get("message"));
			}
			
			hwRegistration = context.registerService(HelloWorld.class.getName(), hw, null);
		}
	}
}
