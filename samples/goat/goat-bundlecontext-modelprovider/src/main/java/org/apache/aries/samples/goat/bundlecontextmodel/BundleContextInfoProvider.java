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
package org.apache.aries.samples.goat.bundlecontextmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

import org.apache.aries.samples.goat.info.ComponentInfoImpl;
import org.apache.aries.samples.goat.info.RelationshipInfoImpl;

import org.apache.aries.samples.goat.api.ComponentInfo;
import org.apache.aries.samples.goat.api.ComponentInfoProvider;
import org.apache.aries.samples.goat.api.ModelInfoService;
import org.apache.aries.samples.goat.api.RelationshipAspect;
import org.apache.aries.samples.goat.api.RelationshipInfo;
import org.apache.aries.samples.goat.api.RelationshipInfoProvider;

public class BundleContextInfoProvider implements ModelInfoService, RelationshipInfoProvider, ComponentInfoProvider, BundleListener, ServiceListener {
	
	private Map<String, ComponentInfo>biCache = new HashMap<String,ComponentInfo>();
	
	private Map<String, RelationshipInfo>riCache = new HashMap<String,RelationshipInfo>();
	
	private List<ComponentInfoListener> clisteners=null;
	private List<RelationshipInfoListener> rlisteners=null;

	private BundleContext ctx=null;
	
	public BundleContextInfoProvider(BundleContext ctx){
		System.err.println("BCIP built!");
		this.ctx = ctx;
		this.clisteners = Collections.synchronizedList(new ArrayList<ComponentInfoListener>());
		this.rlisteners = Collections.synchronizedList(new ArrayList<RelationshipInfoListener>());
		this.ctx.addBundleListener(this);		
		this.ctx.addServiceListener(this);		
	}
	
	

	public List<ComponentInfo> getComponents() {
		System.err.println("BCIP getBundles called");
		Bundle[] bundles = this.ctx.getBundles();
		List<ComponentInfo> result = new ArrayList<ComponentInfo>();
		
		for(int i=0; i<bundles.length; i++){
			System.err.println("BCIP converting "+i);
			result.add( getComponentForId( getKeyForBundle(bundles[i])) );
		}

		System.err.println("BCIP returning data");
		return result;
	}


	public void registerComponentInfoListener(ComponentInfoListener listener) {
		clisteners.add(listener);
	}
	
	public void registerRelationshipInfoListener(RelationshipInfoListener listener) {
		rlisteners.add(listener);
	}

	
	private Bundle getBundleForIDKey(BundleContext ctx, String id){
		String s =id.substring("/root/".length());
		Long l = Long.parseLong(s);
		return ctx.getBundle(l.longValue());
	}

	private String bundleStateToString(int bundleState){
		switch(bundleState){
		case Bundle.UNINSTALLED : return "UNINSTALLED";
		case Bundle.INSTALLED : return "INSTALLED";
		case Bundle.RESOLVED : return "RESOLVED";
		case Bundle.STARTING : return "STARTING";
		case Bundle.STOPPING : return "STOPPING";
		case Bundle.ACTIVE : return "ACTIVE";
		default : return "UNKNOWN["+bundleState+"]";
		}
	} 
	
	public ComponentInfo getComponentForId(String id) {
		if(biCache.containsKey(id)){
			return biCache.get(id);
		}
		
		Bundle b = getBundleForIDKey(ctx,id);		
		ComponentInfoImpl bii = new ComponentInfoImpl();
		
		bii.setId(getKeyForBundle(b));
		
		HashSet<Long> allDepSet = new HashSet<Long>();
		
		bii.setComponentProperties(new HashMap<String,String>());
		
		bii.getComponentProperties().put("BundleID", ""+b.getBundleId());
		bii.getComponentProperties().put("State", bundleStateToString(b.getState()));
		bii.getComponentProperties().put("SymbolicName", b.getSymbolicName());
		bii.getComponentProperties().put("Version", ""+b.getVersion());
		
		Enumeration<String> e = b.getHeaders().keys();
		while(e.hasMoreElements()){
			String key = e.nextElement();

			//Ideally we'd add everything here.. but until we add the filtering in the ui
			//its easier to just filter here.. for now, all 'extra' properties are removed.

			if(! (key.equals("Import-Package") || key.equals("Export-Package")) ){
			  //bii.getComponentProperties().put(key, String.valueOf(b.getHeaders().get(key)));
			}
		}
		
		bii.setChildren(new ArrayList<ComponentInfo>());
		
		biCache.put(id, bii);		
		return bii;
	}



	public void bundleChanged(BundleEvent arg0) {
		String id = getKeyForBundle(arg0.getBundle());
		if(biCache.containsKey(id)){
			biCache.remove(id);
		}

		ComponentInfo bi = getComponentForId(getKeyForBundle(arg0.getBundle()));
		
		for(ComponentInfoListener bil : clisteners){
			bil.updateComponent(bi);
		}
		
	}

	private String getKeyForBundle(Bundle b){
		return "/root/"+b.getBundleId();
	}

	@Override
	public List<RelationshipInfo> getRelationships() {
		
		ArrayList<RelationshipInfo> r = new ArrayList<RelationshipInfo>();
		
		Bundle bundles[] = ctx.getBundles();
		PackageAdmin pa = (PackageAdmin)ctx.getService(ctx.getServiceReference(PackageAdmin.class.getName().toString()));
		
		if(bundles!=null && bundles.length!=0){
			for(Bundle b: bundles){
				String bkey = getKeyForBundle(b);
				ComponentInfo ci = getComponentForId(bkey);
				
				//add all the packages..
				//we only add exports, as imports are implied in the reverse
				ExportedPackage eps[] = pa.getExportedPackages(b);
				if(eps!=null && eps.length!=0){
					for(ExportedPackage ep : eps){
						RelationshipInfoImpl ri = new RelationshipInfoImpl();
						ri.setProvidedBy( ci );
						ri.setType("Package");
						ri.setName(ep.getName());
						ri.setRelationshipAspects(new ArrayList<RelationshipAspect>());
						ri.setConsumedBy(new ArrayList<ComponentInfo>());
						//TODO: add versioning aspect.
						Bundle imps[] = ep.getImportingBundles();
						if(imps!=null && imps.length!=0){
							for(Bundle imp : imps){
								ri.getConsumedBy().add(getComponentForId(getKeyForBundle(imp)));
							}
						}
						r.add(ri);						
					}
				}
			
				//add all the services.. 
				//we only add registered services, as ones in use are handled in the reverse
				ServiceReference srs[] = b.getRegisteredServices();
				if(srs!=null && srs.length!=0){
					for(ServiceReference sr : srs){	
						RelationshipInfoImpl ri = getRIforSR(sr);
						ri.setProvidedBy( ci );
						r.add(ri);
					}
				}
				
			}
		}
		
		
		return r;
	}

    private RelationshipInfoImpl getRIforSR(ServiceReference sr){
		RelationshipInfoImpl ri = new RelationshipInfoImpl();
		ri.setType("Service");
		String serviceNames="";
		String []objectClasses = (String[])sr.getProperty("objectClass");
		if(objectClasses!=null){
			for(String objectClass : objectClasses){
				serviceNames+=","+objectClass;
			}
		}
		if(serviceNames.length()>1){
			serviceNames = serviceNames.substring(1);
		}
		
		ri.setName(serviceNames);
		ri.setRelationshipAspects(new ArrayList<RelationshipAspect>());
		//TODO: add service parameters
		ri.setConsumedBy(new ArrayList<ComponentInfo>());
		
		Bundle using[] = sr.getUsingBundles();
		if(using!=null && using.length!=0){
			for(Bundle u : using){
				ri.getConsumedBy().add(getComponentForId(getKeyForBundle(u)));
			}
		}
        return ri;
    }

	@Override
	public String getName() {
		return "Bundle Context Info Provider 1.0";
	}



	@Override
	public ComponentInfoProvider getComponentInfoProvider() {
		return this;
	}



	@Override
	public RelationshipInfoProvider getRelationshipInfoProvider() {
		return this;
	}



	@Override
	public void serviceChanged(ServiceEvent arg0) {
		if(arg0.getType() == ServiceEvent.REGISTERED || arg0.getType() == ServiceEvent.MODIFIED || arg0.getType() == ServiceEvent.MODIFIED_ENDMATCH){
			ServiceReference sr = arg0.getServiceReference();
			RelationshipInfoImpl ri = getRIforSR(sr);
			ComponentInfo ci = getComponentForId(getKeyForBundle(sr.getBundle()));
			ri.setProvidedBy(ci);
			
			for(RelationshipInfoListener ril : rlisteners){
				ril.updateRelationship(ri);
			}
		}else if(arg0.getType() == ServiceEvent.UNREGISTERING){
			ServiceReference sr = arg0.getServiceReference();
			RelationshipInfoImpl ri = getRIforSR(sr);
			ComponentInfo ci = getComponentForId(getKeyForBundle(sr.getBundle()));
			ri.setProvidedBy(ci);
			
			for(RelationshipInfoListener ril : rlisteners){
				ril.removeRelationship(ri);
			}			
		}
		

	}

}
