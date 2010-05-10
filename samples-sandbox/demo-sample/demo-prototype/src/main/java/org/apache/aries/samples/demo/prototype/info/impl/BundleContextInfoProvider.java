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
package org.apache.aries.samples.demo.prototype.info.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValuePair;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.apache.aries.samples.demo.prototype.info.BundleInfo;
import org.apache.aries.samples.demo.prototype.info.BundleInfoProvider;

public class BundleContextInfoProvider implements BundleInfoProvider, BundleListener {
	
	private List<BundleInfoListener> listeners=null;

	private BundleContext ctx=null;
	
	public BundleContextInfoProvider(BundleContext ctx){
		this.ctx = ctx;
		this.listeners = Collections.synchronizedList(new ArrayList<BundleInfoListener>());
		this.ctx.addBundleListener(this);
	}
	
	
	// @Override
	public BundleInfo[] getBundles() {
		Bundle[] bundles = this.ctx.getBundles();
		BundleInfo[] result = new BundleInfo[bundles.length];
		
		for(int i=0; i<bundles.length; i++){
			result[i] = getBundleForId(bundles[i].getBundleId());
		}

		return result;
	}

	// @Override
	public void registerBundleInfoListener(BundleInfoListener listener) {
		listeners.add(listener);
	}


	// @Override
	public BundleInfo getBundleForId(long id) {
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
				
				long sid=0;
				allDepSet.add(sid);				
				pii.setSuppliedBy(sid);
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
		String exportString = (String)b.getHeaders().get("Export-Package");
		if(exportString!=null){
			List<NameValuePair<String, NameValueMap<String, String>>> exports = ManifestHeaderProcessor.parseExportString(exportString);
			for(NameValuePair<String, NameValueMap<String, String>> export : exports){
				
				BundleInfoImpl.PackageInfoImpl pii = new BundleInfoImpl.PackageInfoImpl();
				pii.setName(export.getName());		
				
				long sid=0;
				allDepSet.add(sid);				
				pii.setSuppliedBy(sid);
				pii.setParameters(new ArrayList<String[]>());
				pii.setSupplyCandidates(new long[]{});
				pii.setVersion("");
				
				exps.add(pii);
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
		
		return bii;
	}


	// @Override
	public void bundleChanged(BundleEvent arg0) {
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
