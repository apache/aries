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
import org.directwebremoting.ScriptBuffer;
import org.directwebremoting.ScriptSession;
import org.directwebremoting.ServerContextFactory;
import org.directwebremoting.ui.dwr.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.apache.aries.samples.demo.api.BundleInfo;
import org.apache.aries.samples.demo.api.BundleInfoProvider;

public class ServerSideClass implements Runnable {

	private String bundleInfoProviderHint="";
	private String bundleInfoProviderOptions="";
	
	private BundleInfoProvider bundleInfoProvider=null;
	
	private Map<BundleInfoProvider, BundleInfoProvider.BundleInfoListener>listeners=new HashMap<BundleInfoProvider,BundleInfoProvider.BundleInfoListener>();

    protected transient boolean active = false;
	
	private static Map<String, String> coords = new HashMap<String, String>();
	
    private final class UpdatePageInvocation implements Runnable {
    	
		private final String output;

		private UpdatePageInvocation(String output) {
			this.output = output;
		}

		public void run() {
			//add the clock update
		    Util.setValue("displayMessage", "Server Time is : "+String.valueOf(output) );
		    
		    //make test fn that does the blue/red indicator
		    addFunctionCall("testJS", output);		   		    
		}
	}

    private class ListenerImpl implements BundleInfoProvider.BundleInfoListener{
    	String server;
    	String options;
    	public ListenerImpl(String server, String options){
    		this.server=server;
    		this.options=options;
    	}
    	public void addNewBundleInfo(BundleInfo b) {
    		if(this.server.equals(bundleInfoProviderHint) &&
    		   this.options.equals(bundleInfoProviderOptions)){
    			   //todo: only issue the add for the new bundle, and affected other bundles.
    			   getInitialBundles(bundleInfoProviderHint, bundleInfoProviderOptions);
    		   }	
    	}


    	public void updateBundleInfo(BundleInfo b) {
    		if(this.server.equals(bundleInfoProviderHint) &&
 		   this.options.equals(bundleInfoProviderOptions)){
 			   //todo: only issue the add for the new bundle, and affected other bundles.
 			   getInitialBundles(bundleInfoProviderHint, bundleInfoProviderOptions);
 		   }
    	}

    	
    }
    
	public ServerSideClass() {
		System.err.println("SSC Built!");
		
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS); 
       	
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
		default : return "UNKNOWN["+bundleState+"]";
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
    public void getInitialBundles(String dataProvider, String options){
    	
    	System.err.println("GET INITIAL BUNDLES ASKED TO USE DATAPROVIDER "+dataProvider+" OPTIONS "+options);
    	
    	if(dataProvider==null)
    		throw new IllegalArgumentException("Unable to accept 'null' as a dataProvider");
    	
    	//do we need to update?
    	if( !this.bundleInfoProviderHint.equals(dataProvider) ||
    	    !this.bundleInfoProviderOptions.equals(options)){
    		    		
    		this.bundleInfoProviderHint = dataProvider;
    		this.bundleInfoProviderOptions = options;
    		if(this.bundleInfoProviderOptions==null){
    			this.bundleInfoProviderOptions="";
    		}    		    		
    		
    		if(!(this.bundleInfoProvider==null)){
    			//we already had a provider.. we need to shut down the existing bundles in the browsers..
    			addFunctionCall("forgetAboutAllBundles");
    		}
    		
        	ServletContext context = org.directwebremoting.ServerContextFactory.get().getServletContext();
    		Object o = context.getAttribute("osgi-bundlecontext");
    		if(o!=null){
    			if(o instanceof BundleContext){
    				BundleContext b_ctx = (BundleContext)o;
    				
    				System.err.println("Looking up bcip");
    				try{
    				ServiceReference sr[] = b_ctx.getServiceReferences(BundleInfoProvider.class.getName(), "(displayName="+this.bundleInfoProviderHint+")");
    				if(sr!=null){
    					System.err.println("Getting bcip");
    					this.bundleInfoProvider = (BundleInfoProvider)b_ctx.getService(sr[0]);
    					System.err.println("Got bcip "+this.bundleInfoProvider);
    				}else{
    					System.err.println("UNABLE TO FIND BCIP!!");
    					System.err.println("UNABLE TO FIND BCIP!!");
    					System.err.println("UNABLE TO FIND BCIP!!");
    				}
    				}catch(InvalidSyntaxException ise){
    					
    				}
    				//this.bundleInfoProvider = new BundleContextInfoProvider(b_ctx);
    				if(this.bundleInfoProvider!=null){
    					if(!listeners.containsKey(this.bundleInfoProvider)){    
    						BundleInfoProvider.BundleInfoListener l = new ListenerImpl(this.bundleInfoProviderHint, this.bundleInfoProviderOptions);
    						listeners.put(this.bundleInfoProvider, l);
    						this.bundleInfoProvider.registerBundleInfoListener(l);
    					}
    				}
    			}
    		}
    		
    	}
    	
    	
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
    			   			
    			System.err.println("Adding dependencies");
    			Set<Long> dependencies = new HashSet<Long>();
    			for(long id : b.getDependencies() ){
    				System.err.println(".. "+id);
    				dependencies.add(id);
    			}
    			
    			System.err.println("Adding impdeps");
    			Set<Long> impDeps = new HashSet<Long>();
    			for(BundleInfo.PackageInfo p: b.getImportedPackages()){
    				System.err.println(".. "+p.getSuppliedBy()+" "+String.valueOf(p));
    				if(p.getSuppliedBy() != -1){
    					impDeps.add(p.getSuppliedBy());
    				}
    			}
    			
    			System.err.println("Adding expdeps");
    			Set<Long> expDeps = new HashSet<Long>();
    			for(BundleInfo.PackageInfo p: b.getExportedPackages()){
    				if(p.getUsedBy()!=null){
	    				for(long l : p.getUsedBy()){
		    				System.err.println(".. "+l+" "+String.valueOf(p));
		    				if(l != -1){
		    					expDeps.add(l);
		    				}
	    				}
    				}
    			}
    			
    			allDep.put(b.getBundleId(), dependencies);
    			allImpDep.put(b.getBundleId(), impDeps);
    			allExpDep.put(b.getBundleId(), expDeps);
    		}
    	}    	
    	
    	System.err.println("MAP "+allImpDep+"  "+allExpDep);
    	
    	bd.setTotalDependencies(allDep);
    	bd.setPackageImportDependencies(allImpDep);
    	bd.setPackageExportDependencies(allExpDep);
		addFunctionCall("setBundleDependencies",bd);  
		
    }
    
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
    	
    	System.err.println("Got "+bi+" for "+bundleId);    	
    	System.err.println(".. "+bi.getSymbolicName());    	
    	System.err.println("exp packages is "+String.valueOf(bi.getExportedPackages()));
    	
    	ArrayList<String> exps = new ArrayList<String>();
		for(BundleInfo.PackageInfo p: bi.getExportedPackages()){
			
			System.err.println(p);
			
			System.err.println("processing "+p.getName());
			
			if(p.getSuppliedBy() != -1){
				System.err.println("adding to list");
				exps.add(p.getName());
			}else{
				System.err.println("ignoring -1");
			}
		}
		
		System.err.println("converting to array");
		String[] result = new String[exps.size()];
		if(result.length==0){
			System.err.println("returning no exports");
			result = new String[]{"No Exports"};
		}else{
			System.err.println("calling toArray "+exps.size());
			result = exps.toArray(result);
			System.err.println("returning "+result.length+" results");
		}
    	return result;
    }


	
	public String[] getProviders(){
		System.err.println("Getting providers...");
		ArrayList<String> result=new ArrayList<String>();
    	ServletContext context = ServerContextFactory.get().getServletContext();
		Object o = context.getAttribute("osgi-bundlecontext");
		if(o!=null){
			if(o instanceof BundleContext){
				BundleContext b_ctx = (BundleContext)o;
				try{
					System.err.println("Getting providers [2]...");
					ServiceReference[] srs = b_ctx.getServiceReferences(BundleInfoProvider.class.getName(), null);
					System.err.println("Got.. "+srs);
					if(srs==null || srs.length==0){
						System.err.println("NO DATA PROVIDERS");
						throw new RuntimeException("Unable to find any data providers");
					}
					System.err.println("Processing srs as loop.");
					for(ServiceReference sr : srs){
						System.err.println("Processing srs entry...");
						
						String name = (String.valueOf(sr.getProperty("displayName")));
						
						result.add(name);
					}	
					System.err.println("Processed srs as loop.");
				}catch(InvalidSyntaxException e){
					//wont happen, the exception relates to the filter, (2nd arg above), which is constant null.
				}
			}
		}	
		System.err.println("Returning "+result.size());
		String[] arr = new String[result.size()];
		arr = result.toArray(arr);
		for(String x: arr){
			System.err.println(" - "+x);
		}
		return arr;
	}
}
