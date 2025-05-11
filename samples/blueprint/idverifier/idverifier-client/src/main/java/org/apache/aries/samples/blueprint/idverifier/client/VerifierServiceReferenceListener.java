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
import org.apache.aries.samples.blueprint.idverifier.server.PersonIDVerifierComplexImpl;
import org.apache.aries.samples.blueprint.idverifier.server.PersonIDVerifierSimpleImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author forrestxm
 *
 */
public class VerifierServiceReferenceListener {
    private static final Logger log = LoggerFactory.getLogger(VerifierServiceReferenceListener.class);

	public void bind(ServiceReference svcref) {
		log.info("**********" + this.getClass().getSimpleName() + " bind method via ServiceReference!*********");
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
		log.info("Bundle " + svcproviderbundle.getSymbolicName() + " provides this service implemented by " + svcbeanname);
		// Print service users information
		log.info("**********Start of printing service's users**********");
		Bundle[] usingbundles = svcref.getUsingBundles();
		if (usingbundles != null) {
			int len = usingbundles.length;
			log.info("The service has " + len + " users!");
			log.info("They are:");
			for (int i = 0; i < len; i++) {
				log.info(usingbundles[i].getSymbolicName());
			}
			log.info("All users are printed out!");
		}
		log.info("**********End of printing service's users**********");
		
	}

	public void bind(PersonIDVerifier svc) {
		log.info("**********This is service object proxy bind method!***********");
	}
	
	public void unbind(ServiceReference svcref) {
		log.info("**********" + this.getClass().getSimpleName() + " unbind method via ServiceReference!*********");
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
		log.info("Bundle " + svcproviderbundle.getSymbolicName() + " provides this service implemented by " + svcbeanname);
		// Print service users information
		log.info("**********Start of printing service's users**********");
		Bundle[] usingbundles = svcref.getUsingBundles();
		if (usingbundles != null) {
			int len = usingbundles.length;
			log.info("The service has " + len + " users!");
			log.info("They are:");
			for (int i = 0; i < len; i++) {
				log.info(usingbundles[i].getSymbolicName());
			}
			log.info("All users are printed out!");
		}
		log.info("**********End of printing service's users**********");
		
	}

	public void unbind(PersonIDVerifier svc, Map props) {
		log.info("**********This is service object proxy unbind method!***********");
		log.info("**********Start of printing service properties***********");
		log.info("Service properties are:");
		Set keys = props.keySet();
		for (Object obj : keys) {
			Object valueobj = props.get(obj);
			log.info(obj + "=" + valueobj);
		}
		log.info("**********End of printing service properties***********");
	}

}
