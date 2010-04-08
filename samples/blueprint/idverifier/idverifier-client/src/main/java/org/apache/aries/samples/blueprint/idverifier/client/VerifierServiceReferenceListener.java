/**
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
package org.apache.aries.samples.blueprint.idverifier.client;

import java.util.Map;
import java.util.Set;


import org.apache.aries.samples.blueprint.idverifier.api.PersonIDVerifier;
import org.apache.aries.samples.blueprint.idverifier.server.PersonIDVerifierSimpleImpl;
import org.apache.aries.samples.blueprint.idverifier.server.PersonIDVerifierComplexImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author forrestxm
 *
 */
public class VerifierServiceReferenceListener {
	public void bind(ServiceReference svcref) {
		System.out.println("**********" + this.getClass().getSimpleName() + " bind method via ServiceReference!*********");
		// Get specific PersonIDVerifier implementation class
		Bundle svcproviderbundle = svcref.getBundle();
		BundleContext svcproviderbundlectx = svcproviderbundle.getBundleContext();
		Object svcbean = svcproviderbundlectx.getService(svcref);
		String svcbeanname = null;
		if (svcbean instanceof PersonIDVerifierSimpleImpl) {
			svcbeanname = ((PersonIDVerifierSimpleImpl)svcbean).getClass().getCanonicalName();
		} else if (svcbean instanceof PersonIDVerifierComplexImpl){
			svcbeanname = ((PersonIDVerifierComplexImpl)svcbean).getClass().getCanonicalName();
		}
		System.out.println("Bundle " + svcproviderbundle.getSymbolicName() + " provides this service implemented by " + svcbeanname);
		// Print service users information
		System.out.println("**********Start of printing service's users**********");
		Bundle[] usingbundles = svcref.getUsingBundles();
		if (usingbundles != null) {
			int len = usingbundles.length;
			System.out.println("The service has " + len + " users!");
			System.out.println("They are:");
			for (int i = 0; i < len; i++) {
				System.out.println(usingbundles[i].getSymbolicName());
			}
			System.out.println("All users are printed out!");
		}
		System.out.println("**********End of printing service's users**********");
		
	}

	public void bind(PersonIDVerifier svc) {
		System.out.println("**********This is service object proxy bind method!***********");
	}
	
	public void unbind(ServiceReference svcref) {
		System.out.println("**********" + this.getClass().getSimpleName() + " unbind method via ServiceReference!*********");
		// Get specific PersonIDVerifier implementation class
		Bundle svcproviderbundle = svcref.getBundle();
		BundleContext svcproviderbundlectx = svcproviderbundle.getBundleContext();
		Object svcbean = svcproviderbundlectx.getService(svcref);
		String svcbeanname = null;
		if (svcbean instanceof PersonIDVerifierSimpleImpl) {
			svcbeanname = ((PersonIDVerifierSimpleImpl)svcbean).getClass().getCanonicalName();
		} else if (svcbean instanceof PersonIDVerifierComplexImpl){
			svcbeanname = ((PersonIDVerifierComplexImpl)svcbean).getClass().getCanonicalName();
		}
		System.out.println("Bundle " + svcproviderbundle.getSymbolicName() + " provides this service implemented by " + svcbeanname);
		// Print service users information
		System.out.println("**********Start of printing service's users**********");
		Bundle[] usingbundles = svcref.getUsingBundles();
		if (usingbundles != null) {
			int len = usingbundles.length;
			System.out.println("The service has " + len + " users!");
			System.out.println("They are:");
			for (int i = 0; i < len; i++) {
				System.out.println(usingbundles[i].getSymbolicName());
			}
			System.out.println("All users are printed out!");
		}
		System.out.println("**********End of printing service's users**********");
		
	}

	public void unbind(PersonIDVerifier svc, Map props) {
		System.out.println("**********This is service object proxy unbind method!***********");
		System.out.println("**********Start of printing service properties***********");
		System.out.println("Service properties are:");
		Set keys = props.keySet();
		for (Object obj : keys) {
			Object valueobj = props.get(obj);
			System.out.println(obj + "=" + valueobj);
		}
		System.out.println("**********End of printing service properties***********");
	}

}
