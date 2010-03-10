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
package org.apache.aries.tutorials.blueprint.greeter.client.osgi;

import org.apache.aries.tutorials.blueprint.greeter.api.GreeterMessageService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Example of various approaches to using an osgi service.
 */
public class GreeterTestClient {
    String clientID = "<unset>";
    ServiceTracker tracker = null;
    GreeterMessageService cachedService = null;
    BundleContext owningContext;
    
    GreeterMessageService proxy = null;
    
    /**
     * A simple class to hide if a service is available to invoke.
     */
    class GreeterMessageServiceProxy implements GreeterMessageService{
        ServiceTracker greetingServiceTracker = null;
        public GreeterMessageServiceProxy(BundleContext ctx){
            greetingServiceTracker = new ServiceTracker(ctx, GreeterMessageService.class.getName(), null);
            greetingServiceTracker.open();
        }
        public String getGreetingMessage(){
            if(greetingServiceTracker.getTrackingCount()>0){
                return ((GreeterMessageService)greetingServiceTracker.getService()).getGreetingMessage();
            }else{
                return "<No Service Available>";
            }
            
        }
    }
    
    public GreeterTestClient(BundleContext ctx, String clientID){
     this.clientID = clientID;
     owningContext = ctx;
     
     //setup for proxied service usage.
     proxy = new GreeterMessageServiceProxy(ctx);
     
     //setup for tracker usage.
     tracker = new ServiceTracker(ctx, GreeterMessageService.class.getName(), null);
     tracker.open();
     
     //setup for super optimistic osgi service usage ;-)
     try{
         cachedService  = (GreeterMessageService)ctx.getService(ctx.getServiceReference(GreeterMessageService.class.getName()));
     }catch(RuntimeException e){
         System.err.println("Unable to cache service at bundle start!");
     }
    }
    
    public void doRequests(){
        System.out.println("Proxy based...");
        doRequestUsingProxy();
        System.out.println("Tracker based...");
        doRequestUsingTracker();
        System.out.println("Immediate Lookup... ");
        doRequestUsingImmediateLookup(owningContext);
        System.out.println("Extremely hopeful...");
        doRequestUsingCachedService();
    }
    
    private void doRequestUsingImmediateLookup(BundleContext ctx){
        ServiceReference ref = ctx.getServiceReference(GreeterMessageService.class.getName());
        if(ref!=null){
            GreeterMessageService greetingService = (GreeterMessageService)ctx.getService(ref);
            if(greetingService!=null){
                String response = greetingService.getGreetingMessage();
                printResult(response);
            }else{
                System.err.println("Immediate lookup gave null for getService");
            }
            ctx.ungetService(ref); // dont forget this!!
        }else{
            System.err.println("Immediate lookup gave null service reference");
        }
    }
    
    private void doRequestUsingTracker(){
        System.out.println("Tracker is tracking.. "+tracker.getTrackingCount()+" services");
        GreeterMessageService greetingService = (GreeterMessageService)tracker.getService();
        if(greetingService!=null){
            String response = greetingService.getGreetingMessage();
            printResult(response);
        }else{
            System.err.println("Tracker gave null for service..");
        }
    }
    
    private void doRequestUsingCachedService(){
        try{
            printResult(cachedService.getGreetingMessage());
        }catch(RuntimeException e){
            System.err.println("Unable to use cached service. "+e);
        }
    }
    
    private void doRequestUsingProxy(){
        try{
            printResult(proxy.getGreetingMessage());
        }catch(RuntimeException e){
            System.err.println("Unable to use proxied service. "+e);
        }
    }
    
    private void printResult(String response){
        System.out.println("**********************************");
        System.out.println("*"+clientID);
        System.out.println("*"+response);
        System.out.println("**********************************");
    }

}
