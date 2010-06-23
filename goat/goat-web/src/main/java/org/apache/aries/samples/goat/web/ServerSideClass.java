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
package org.apache.aries.samples.goat.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.aries.samples.goat.api.ComponentInfo;
import org.apache.aries.samples.goat.api.ComponentInfoProvider;
import org.apache.aries.samples.goat.api.ModelInfoService;
import org.apache.aries.samples.goat.api.RelationshipInfo;
import org.apache.aries.samples.goat.api.RelationshipInfoProvider;
import org.directwebremoting.Browser;
import org.directwebremoting.ScriptBuffer;
import org.directwebremoting.ScriptSession;
import org.directwebremoting.ServerContextFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class ServerSideClass {

	private String modelInfoServiceHint = "";

	private ModelInfoService ModelInfoService = null;

	private Map<ModelInfoService, ComponentInfoProvider.ComponentInfoListener> clisteners = new HashMap<ModelInfoService, ComponentInfoProvider.ComponentInfoListener>();
	private Map<ModelInfoService, RelationshipInfoProvider.RelationshipInfoListener> rlisteners = new HashMap<ModelInfoService, RelationshipInfoProvider.RelationshipInfoListener>();

	private class ComponentInfoListenerImpl implements
			ComponentInfoProvider.ComponentInfoListener {
		String server;

		public ComponentInfoListenerImpl(String server) {
			this.server = server;
		}

		public void updateComponent(ComponentInfo b) {
			if (this.server.equals(modelInfoServiceHint)) {
				// todo: only issue the add for the new bundle, and affected
				// other bundles.
				//getInitialComponents(modelInfoServiceHint);
				//System.out.println("State is: " + b.getComponentProperties().get("State"));
				addFunctionCall("addComponent", b);
			}
		}

		public void removeComponent(ComponentInfo b) {
			// todo
		}
	}
	private class RelationshipInfoListenerImpl implements
			RelationshipInfoProvider.RelationshipInfoListener {
		String server;

		public RelationshipInfoListenerImpl(String server) {
			this.server = server;
		}

		public void updateRelationship(RelationshipInfo r) {
			if (this.server.equals(modelInfoServiceHint)) {
				addFunctionCall("addRelationship", r);
			}
		}

		public void removeRelationship(RelationshipInfo r) {
			// todo
		}
	}

	public ServerSideClass() {
		System.err.println("SSC Built!");

	}

	@SuppressWarnings("unused")
	private String bundleStateToString(int bundleState) {
		switch (bundleState) {
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.STOPPING:
			return "STOPPING";
		case Bundle.ACTIVE:
			return "ACTIVE";
		default:
			return "UNKNOWN[" + bundleState + "]";
		}
	}

	/**
	 * this is invoked by a page onload.. so until it's invoked.. we dont care
	 * about components
	 */
	public void getInitialComponents(String dataProvider) {

		System.err.println("GET INITIAL BUNDLES ASKED TO USE DATAPROVIDER "
				+ dataProvider);

		if (dataProvider == null)
			throw new IllegalArgumentException(
					"Unable to accept 'null' as a dataProvider");

		// do we need to update?
		if (!this.modelInfoServiceHint.equals(dataProvider)) {

			this.modelInfoServiceHint = dataProvider;

			if (!(this.ModelInfoService == null)) {
				// we already had a provider.. we need to shut down the existing
				// components & relationships in the browsers..
				addFunctionCall("forgetAboutEverything");
			}

			ServletContext context = org.directwebremoting.ServerContextFactory
					.get().getServletContext();
			Object o = context.getAttribute("osgi-bundlecontext");
			if (o != null) {
				if (o instanceof BundleContext) {
					BundleContext b_ctx = (BundleContext) o;

					System.err.println("Looking up bcip");
					try {
						ServiceReference sr[] = b_ctx.getServiceReferences(
								ModelInfoService.class.getName(),
								"(displayName=" + this.modelInfoServiceHint
										+ ")");
						if (sr != null) {
							System.err.println("Getting bcip");
							this.ModelInfoService = (ModelInfoService) b_ctx
									.getService(sr[0]);
							System.err.println("Got bcip "
									+ this.ModelInfoService);
						} else {
							System.err.println("UNABLE TO FIND BCIP!!");
							System.err.println("UNABLE TO FIND BCIP!!");
							System.err.println("UNABLE TO FIND BCIP!!");
						}
					} catch (InvalidSyntaxException ise) {

					}

					if (this.ModelInfoService != null) {
						if (!rlisteners.containsKey(this.ModelInfoService)) {
							RelationshipInfoProvider.RelationshipInfoListener rl = new RelationshipInfoListenerImpl(
									this.modelInfoServiceHint);
							rlisteners.put(this.ModelInfoService, rl);
							this.ModelInfoService.getRelationshipInfoProvider()
									.registerRelationshipInfoListener(rl);
						}

						if (!clisteners.containsKey(this.ModelInfoService)) {
							ComponentInfoProvider.ComponentInfoListener cl = new ComponentInfoListenerImpl(
									this.modelInfoServiceHint);
							clisteners.put(this.ModelInfoService, cl);
							this.ModelInfoService.getComponentInfoProvider()
									.registerComponentInfoListener(cl);
						}
					}
				}
			}

		}

		Collection<ComponentInfo> bis = this.ModelInfoService
				.getComponentInfoProvider().getComponents();
		System.err.println("Got " + (bis == null ? "null" : bis.size())
				+ " components back from the provider ");
		if (bis != null) {
			for (ComponentInfo b : bis) {

				System.err.println("Adding Component .. " + b.getId());

				addFunctionCall("addComponent", b);
			}
		}

		Collection<RelationshipInfo> ris = this.ModelInfoService
				.getRelationshipInfoProvider().getRelationships();
		System.err.println("Got " + (ris == null ? "null" : ris.size())
				+ " relationships back from the provider ");
		if (ris != null) {
			for (RelationshipInfo r : ris) {
				System.err.println("Adding relationship type " + r.getType()
						+ " called " + r.getName() + " from "
						+ r.getProvidedBy().getId());

				addFunctionCall("addRelationship", r);
			}
		}

	}

	private void addFunctionCall(String name, Object... params) {
		final ScriptBuffer script = new ScriptBuffer();
		script.appendScript(name).appendScript("(");
		for (int i = 0; i < params.length; i++) {
			if (i != 0)
				script.appendScript(",");
			script.appendData(params[i]);
		}
		script.appendScript(");");
		Browser.withAllSessions(new Runnable() {
			public void run() {
				for (ScriptSession s : Browser.getTargetSessions()) {
					s.addScript(script);
				}
			}
		});
	}

	public String[] getProviders() {
		System.err.println("Getting providers...");
		ArrayList<String> result = new ArrayList<String>();
		ServletContext context = ServerContextFactory.get().getServletContext();
		Object o = context.getAttribute("osgi-bundlecontext");
		if (o != null) {
			if (o instanceof BundleContext) {
				BundleContext b_ctx = (BundleContext) o;
				try {
					System.err.println("Getting providers [2]...");
					ServiceReference[] srs = b_ctx.getServiceReferences(
							ModelInfoService.class.getName(), null);
					System.err.println("Got.. " + srs);
					if (srs == null || srs.length == 0) {
						System.err.println("NO DATA PROVIDERS");
						throw new RuntimeException(
								"Unable to find any data providers");
					}
					System.err.println("Processing srs as loop.");
					for (ServiceReference sr : srs) {
						System.err.println("Processing srs entry...");

						String name = (String.valueOf(sr
								.getProperty("displayName")));

						result.add(name);
					}
					System.err.println("Processed srs as loop.");
				} catch (InvalidSyntaxException e) {
					// wont happen, the exception relates to the filter, (2nd
					// arg above), which is constant null.
				}
			}
		}
		System.err.println("Returning " + result.size());
		String[] arr = new String[result.size()];
		arr = result.toArray(arr);
		for (String x : arr) {
			System.err.println(" - " + x);
		}
		return arr;
	}
}
