/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.samples.demo.prototype.info.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.apache.aries.samples.demo.api.BundleInfo;
import org.apache.aries.samples.demo.api.BundleInfoProvider;

public class BundleContextInfoProvider implements BundleInfoProvider, BundleListener {
	
	private Map<Long, BundleInfo>biCache = new HashMap<Long,BundleInfo>();
	
	private List<BundleInfoListener> listeners=null;

	private BundleContext ctx=null;
	
	public BundleContextInfoProvider(BundleContext ctx){
		System.err.println("BCIP built!");
		this.ctx = ctx;
		this.listeners = Collections.synchronizedList(new ArrayList<BundleInfoListener>());
		this.ctx.addBundleListener(this);
		
		System.err.println("BUNDLE 0 **********************************************");
		Bundle b = ctx.getBundle(0);
		System.err.println(b.getSymbolicName()+" "+b.getVersion()+" "+b.getLocation());
		
		System.err.println("PROP IS "+System.getProperty("org.osgi.framework.system.packages"));
		
	}
	
	

	public BundleInfo[] getBundles() {
		System.err.println("BCIP getBundles called");
		Bundle[] bundles = this.ctx.getBundles();
		BundleInfo[] result = new BundleInfo[bundles.length];
		
		for(int i=0; i<bundles.length; i++){
			System.err.println("BCIP converting "+i);
			result[i] = getBundleForId(bundles[i].getBundleId());
		}

		System.err.println("BCIP returning data");
		return result;
	}


	public void registerBundleInfoListener(BundleInfoListener listener) {
		listeners.add(listener);
	}



	public BundleInfo getBundleForId(long id) {
		if(biCache.containsKey(id)){
			return biCache.get(id);
		}
		
		Bundle b = ctx.getBundle(id);		
		BundleInfoImpl bii = new BundleInfoImpl();
		
		HashSet<Long> allDepSet = new HashSet<Long>();

		bii.setBundleId(b.getBundleId());
		bii.setState(b.getState());
		bii.setSymbolicName(b.getSymbolicName());
		bii.setVersion(b.getVersion().toString());
				
		ArrayList<BundleInfo.PackageInfo> imps = new ArrayList<BundleInfo.PackageInfo>();
		String importString = (String)b.getHeaders().get("Import-Package");			
		if(importString!=null){								
						
			Map<String, NameValueMap<String, String>> imports = ManifestHeaderProcessor.parseImportString(importString);
			for(Map.Entry<String, NameValueMap<String, String>> packageImport : imports.entrySet()){
				
				BundleInfoImpl.PackageInfoImpl pii = new BundleInfoImpl.PackageInfoImpl();
				pii.setName(packageImport.getKey());
				pii.setSuppliedBy(-1);
				
				ServiceReference sr = ctx.getServiceReference(PackageAdmin.class.getName());
				PackageAdmin pa = (PackageAdmin)ctx.getService(sr);
				
				//am I a fragment ?
				Bundle hosts[] = pa.getHosts(b);
				
				ExportedPackage eps[] = pa.getExportedPackages(pii.getName());
				boolean found=false;
				if(eps!=null&&eps.length>0){
					for(ExportedPackage ep: eps){
						Bundle ibs[] = ep.getImportingBundles();
						if(ibs.length>0){
							for(Bundle ib : ibs){
								Bundle match = null;
								match = (ib.getBundleId() == b.getBundleId()) ? ib : null;								
								//If I'm a fragment, and I haven't found a match yet, perhaps
								//my host has performed the import.
								if(match==null && hosts!=null && hosts.length>0){
									for(Bundle h : hosts){
										if(match==null){
											//is our fragment host, an importer of this package?
											match=(h.getBundleId() == ib.getBundleId())?h:null;
										}
									}
								}								
								if(match!=null){
									//resp.print(indent+"  <bundle-id>"+ep.getExportingBundle().getBundleId()+"</bundle-id>\n");
									long sid=ep.getExportingBundle().getBundleId();
									allDepSet.add(sid);								
									pii.setSuppliedBy(sid);
								}
							}
						}
					}
				}								
			
				pii.setParameters(new ArrayList<String[]>());
				pii.setSupplyCandidates(new long[]{});
				pii.setVersion("");
				
				imps.add(pii);
			}
		}		
		BundleInfo.PackageInfo[] impArr = new BundleInfo.PackageInfo[imps.size()];
		impArr = imps.toArray(impArr);
		bii.setImportedPackages(impArr);
		
		ArrayList<BundleInfo.PackageInfo> exps = new ArrayList<BundleInfo.PackageInfo>();
	
		ServiceReference sr = ctx.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin pa = (PackageAdmin)ctx.getService(sr);

		ExportedPackage[] eps = pa.getExportedPackages(b);
		if(eps!=null&&eps.length>0){
			for(ExportedPackage ep: eps){
				BundleInfoImpl.PackageInfoImpl pii = new BundleInfoImpl.PackageInfoImpl();
				pii.setName(ep.getName());		
				pii.setParameters(new ArrayList<String[]>());
				pii.setSupplyCandidates(new long[]{});
				pii.setVersion("");			
				pii.setUsedBy(new long[0]);
				Bundle ibs[] = ep.getImportingBundles();
				if(ibs.length>0){
					long ids[] = new long[ibs.length];
					int idx=0;
					for(Bundle ib: ibs){
						ids[idx]=ib.getBundleId();
						allDepSet.add(ib.getBundleId());							
					}
					pii.setUsedBy(ids);

					exps.add(pii);
				}
			}
		}
		
		BundleInfo.PackageInfo[] expArr = new BundleInfo.PackageInfo[exps.size()];
		expArr = exps.toArray(expArr);
		bii.setExportedPackages(expArr);
				
		long[] allDepArr = new long[allDepSet.size()];
		int i=0;
		for(Long l: allDepSet){
			allDepArr[i] = l;
			i++;
		}
		bii.setDependencies(allDepArr);
		
		biCache.put(bii.getBundleId(), bii);		
		return bii;
	}



	public void bundleChanged(BundleEvent arg0) {
		long id = arg0.getBundle().getBundleId();
		if(biCache.containsKey(id)){
			BundleInfo bi = biCache.get(id);
			for(long related : bi.getDependencies()){
				if(biCache.containsKey(related)){
					biCache.remove(related);
				}
			}
			biCache.remove(id);
		}		
		BundleInfo bi = getBundleForId(arg0.getBundle().getBundleId());
		
		for(BundleInfoListener bil : listeners){
			if(arg0.getType()==BundleEvent.INSTALLED){
				bil.addNewBundleInfo(bi);
			}else{
				bil.updateBundleInfo(bi);
			}
		}
		
	}

}
