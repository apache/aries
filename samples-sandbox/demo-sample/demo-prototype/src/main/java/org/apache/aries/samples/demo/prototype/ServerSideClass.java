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
package org.apache.aries.samples.demo.prototype;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

import org.directwebremoting.Browser;
import org.directwebremoting.ServerContextFactory;
import org.directwebremoting.ScriptSession;
import org.directwebremoting.ScriptBuffer;
import org.directwebremoting.ui.dwr.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import org.apache.aries.samples.demo.prototype.info.BundleInfo;
import org.apache.aries.samples.demo.prototype.info.BundleInfoProvider;
import org.apache.aries.samples.demo.prototype.info.impl.BundleContextInfoProvider;


public class ServerSideClass implements Runnable, BundleInfoProvider.BundleInfoListener {

	private BundleInfoProvider bundleInfoProvider=null;

    protected transient boolean active = false;
	
    private final class UpdatePageInvocation implements Runnable {
    	
		private final String output;

		private UpdatePageInvocation(String output) {
			this.output = output;
		}

		public void run() {
		    Util.setValue("displayMessage", "Server Time is : "+String.valueOf(output) );
		    
		    //make test fn that does the blue/red indicator
		    addFunctionCall("testJS", output);		   		    
		}
	}

	public ServerSideClass() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS); 
        
    	ServletContext context = ServerContextFactory.get().getServletContext();
		Object o = context.getAttribute("osgi-bundlecontext");
		if(o!=null){
			if(o instanceof BundleContext){
				BundleContext b_ctx = (BundleContext)o;
				this.bundleInfoProvider = new BundleContextInfoProvider(b_ctx);
				this.bundleInfoProvider.registerBundleInfoListener(this);
			}
		}
		
		if(this.bundleInfoProvider==null){
		   throw new RuntimeException("Unable to instantiate the bundleInfoProvider");
		}
    }

    public void run() {
        if (active) {
            setMessageDisplay(new Date().toString());
        }
    }

    /**
     * Called from the client to turn the clock on/off
     */
    public synchronized void toggle() {
        active = !active;

        if (active) {
            setMessageDisplay("Started");
        }
        else {
            setMessageDisplay("Stopped");
        }
    }
    
	private String bundleStateToString(int bundleState){
		switch(bundleState){
		case Bundle.UNINSTALLED : return "UNINSTALLED";
		case Bundle.INSTALLED : return "INSTALLED";
		case Bundle.RESOLVED : return "RESOLVED";
		case Bundle.STARTING : return "STARTING";
		case Bundle.STOPPING : return "STOPPING";
		case Bundle.ACTIVE : return "ACTIVE";
		default : return "UNKNOWN";
		}
	}    

    /**
     * Actually alter the clients.
     * @param output The string to display.
     */
    public void setMessageDisplay(final String output) {
        String page = ServerContextFactory.get().getContextPath() + "/web/index.html";
        Browser.withPage(page, new UpdatePageInvocation(output));
        
    }
    
    /**
     * this is invoked by a page onload.. so until it's invoked.. we dont care
     * about bundles.. 
     */
    public void getInitialBundles(){
    	
    	BundleDependencies bd = new BundleDependencies();
    	
    	Map<Long, Set<Long>> allDep = new HashMap<Long, Set<Long>>();
    	Map<Long, Set<Long>> allImpDep = new HashMap<Long, Set<Long>>();
    	Map<Long, Set<Long>> allExpDep = new HashMap<Long, Set<Long>>();
    	
    	BundleInfo[] bis = this.bundleInfoProvider.getBundles();
    	System.err.println("Got "+bis.length+" bundles back from the provider ");
    	if(bis!=null){
    		for(BundleInfo b: bis){
    	
    			System.err.println("Adding "+b.getBundleId()+" "+b.getSymbolicName()+" "+bundleStateToString(b.getState())+" "+b.getVersion());
    			
    			addFunctionCall("addBundle", 
    					b.getBundleId(), 
    					b.getSymbolicName(), 
    					bundleStateToString(b.getState()), 
    					b.getVersion());
    			   			
    			Set<Long> dependencies = new HashSet<Long>();
    			for(long id : b.getDependencies() ){
    				dependencies.add(id);
    			}
    			
    			Set<Long> impDeps = new HashSet<Long>();
    			for(BundleInfo.PackageInfo p: b.getImportedPackages()){
    				if(p.getSuppliedBy() != -1){
    					impDeps.add(p.getSuppliedBy());
    				}
    			}
    			
    			Set<Long> expDeps = new HashSet<Long>();
    			for(BundleInfo.PackageInfo p: b.getExportedPackages()){
    				if(p.getSuppliedBy() != -1){
    					expDeps.add(p.getSuppliedBy());
    				}
    			}
    			
    			allDep.put(b.getBundleId(), dependencies);
    			allImpDep.put(b.getBundleId(), impDeps);
    			allExpDep.put(b.getBundleId(), expDeps);
    		}
    	}    	
    	bd.setTotalDependencies(allDep);
    	bd.setPackageImportDependencies(allImpDep);
    	bd.setPackageExportDependencies(allExpDep);
		addFunctionCall("setBundleDependencies",bd);   	
    }
    
    public String[] getPackageImports(long bundleId){
    	
    	BundleInfo bi = bundleInfoProvider.getBundleForId(bundleId);
    	
//    	System.err.println("Got "+bi+" for "+bundleId);    	
//    	System.err.println(".. "+bi.getSymbolicName());
//    	System.err.println("imp packages is "+String.valueOf(bi.getExportedPackages()));
    	
    	ArrayList<String> imps = new ArrayList<String>();
		for(BundleInfo.PackageInfo p: bi.getImportedPackages()){
			//System.err.println("processing "+p.getName());
			
			if(p.getSuppliedBy() != -1){
				//System.err.println("adding to list");
				imps.add(p.getName());
			}else{
				//System.err.println("ignoring -1");
			}
		}
		
		//System.err.println("converting to array");
		String[] result = new String[imps.size()];
		if(result.length==0){
			//System.err.println("returning no imports");
			result = new String[]{"No Imports"};
		}else{
			//System.err.println("calling toArray "+imps.size());
			result = imps.toArray(result);
			//System.err.println("returning "+result.length+" results");
		}
    	return result;
    }
    
    public String[] getPackageExports(long bundleId){
    	
    	BundleInfo bi = bundleInfoProvider.getBundleForId(bundleId);
    	
//    	System.err.println("Got "+bi+" for "+bundleId);    	
//    	System.err.println(".. "+bi.getSymbolicName());    	
//    	System.err.println("exp packages is "+String.valueOf(bi.getExportedPackages()));
    	
    	ArrayList<String> exps = new ArrayList<String>();
		for(BundleInfo.PackageInfo p: bi.getExportedPackages()){
			//System.err.println("processing "+p.getName());
			
			if(p.getSuppliedBy() != -1){
				//System.err.println("adding to list");
				exps.add(p.getName());
			}else{
				//System.err.println("ignoring -1");
			}
		}
		
		//System.err.println("converting to array");
		String[] result = new String[exps.size()];
		if(result.length==0){
			//System.err.println("returning no exports");
			result = new String[]{"No Exports"};
		}else{
			//System.err.println("calling toArray "+exps.size());
			result = exps.toArray(result);
			//System.err.println("returning "+result.length+" results");
		}
    	return result;
    }
	//
	//Replacement for ScriptSessions.addFunctionCall
	//
	private void addFunctionCall(String name, Object... params){
		final ScriptBuffer script = new ScriptBuffer();					
		script.appendScript(name).appendScript("(");
		for(int i = 0; i < params.length; i++)
		{
		    if(i != 0)script.appendScript(",");
		    script.appendData(params[i]);
		}
		script.appendScript(");");        
		Browser.withAllSessions(new Runnable(){ public void run(){
			for(ScriptSession s: Browser.getTargetSessions()){
				s.addScript(script);
			}	 
		}});      
	}

	@Override
	public void addNewBundleInfo(BundleInfo b) {
		//todo: only issue the add for the new bundle, and affected other bundles.
		getInitialBundles();
	}

	@Override
	public void updateBundleInfo(BundleInfo b) {
		//todo: only issue the add for the new bundle, and affected other bundles.
		getInitialBundles();
	}
}
