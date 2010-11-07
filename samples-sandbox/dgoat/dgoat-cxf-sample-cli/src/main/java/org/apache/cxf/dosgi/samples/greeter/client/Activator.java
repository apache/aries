/** 
  * Licensed to the Apache Software Foundation (ASF) under one 
  * or more contributor license agreements. See the NOTICE file 
  * distributed with this work for additional information 
  * regarding copyright ownership. The ASF licenses this file 
  * to you under the Apache License, Version 2.0 (the 
  * "License"); you may not use this file except in compliance 
  * with the License. You may obtain a copy of the License at 
  * 
  * http://www.apache.org/licenses/LICENSE-2.0 
  * 
  * Unless required by applicable law or agreed to in writing, 
  * software distributed under the License is distributed on an 
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
  * KIND, either express or implied. See the License for the 
  * specific language governing permissions and limitations 
  * under the License. 
  */
package org.apache.cxf.dosgi.samples.greeter.client;

import java.util.Map;

import org.apache.cxf.dosgi.samples.greeter.GreeterData;
import org.apache.cxf.dosgi.samples.greeter.GreeterException;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.GreetingPhrase;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {    
    private ServiceTracker tracker;

    public void start(final BundleContext bc) {
        tracker = new ServiceTracker(bc, GreeterService.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                Object result = super.addingService(reference);

                useService(bc, reference);
                
                return result;
            }
        };
        tracker.open();
        
    }

    protected void useService(final BundleContext bc, ServiceReference reference) {
        Object svc = bc.getService(reference);
        if (!(svc instanceof GreeterService)) {
            return;
        }
        final GreeterService greeter = (GreeterService) svc;

        Thread t = new Thread(new Runnable() {
            public void run() {
                greeterUI(bc, greeter);
            }
        });
        t.start();
    }

    private void greeterUI(final BundleContext bc, final GreeterService greeter) {
        while (true) {
            System.out.println("*** Opening greeter client dialog ***");
            Object gd = getGreeterData();
            if (gd instanceof String) {
                System.out.println("*** Invoking greeter ***");
                Map<GreetingPhrase, String> result = greeter.greetMe((String) gd);

                System.out.println("greetMe(\"" + gd + "\") returns:");
                for (Map.Entry<GreetingPhrase, String> greeting : result.entrySet()) {
                    System.out.println("  " + greeting.getKey().getPhrase() 
                            + " " + greeting.getValue());
                }
            } else if (gd instanceof GreeterData) {
                System.out.println("*** Invoking greeter ***");
                try {
                    GreetingPhrase [] result = greeter.greetMe((GreeterData) gd);
                    System.out.println("greetMe(\"" + gd + "\") returns:");
                    for (GreetingPhrase phrase : result) {
                        System.out.println("  " + phrase.getPhrase());
                    }
                } catch (GreeterException ex) {
                    System.out.println("GreeterException : " + ex.toString());
                }                
            }
        }
    }

    private static Object getGreeterData() {
        GreeterDialog gd = new GreeterDialog();
        gd.setVisible(true);
        return gd.getSelection();
    }

    public void stop(BundleContext bc) throws Exception {
        tracker.close();
    }
}
