/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.annotation.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.xbean.finder.BundleAnnotationFinder;
import org.apache.aries.blueprint.annotation.service.BlueprintAnnotationScanner;
import org.apache.aries.blueprint.jaxb.Tbean;
import org.apache.aries.blueprint.jaxb.Tproperty;
import org.apache.aries.blueprint.jaxb.Tvalue;
import org.apache.aries.blueprint.jaxb.Tblueprint;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.apache.aries.blueprint.annotation.Bean;
import org.apache.aries.blueprint.annotation.Blueprint;
import org.apache.aries.blueprint.annotation.Inject;
import org.osgi.service.packageadmin.PackageAdmin;

public class BlueprintAnnotationScannerImpl implements BlueprintAnnotationScanner {
    private BundleContext context;
    private List<Class> classes;
    
    public BlueprintAnnotationScannerImpl(BundleContext bc) {
        this.context = bc;    
    }
    public boolean foundBlueprintAnnotation(Bundle bundle) {
        if (this.classes != null && !this.classes.isEmpty()) {
            return true;
        }
        ServiceReference sr = this.context.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin pa = (PackageAdmin)this.context.getService(sr);
        BundleAnnotationFinder baf = null;
        try {
            baf = new BundleAnnotationFinder(pa, bundle);
            // locate the classes that has @blueprint annotation
            this.classes = baf.findAnnotatedClasses(Blueprint.class);
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        

        this.context.ungetService(sr);
        if (this.classes == null || this.classes.isEmpty()) {
            return false;
        }
        
        return true;
    }

    private BundleContext getBlueprintExtenderContext() {
        Bundle[] bundles = this.context.getBundles();
        for (Bundle b : bundles) {
            if (b.getSymbolicName().equals("org.apache.aries.blueprint.core")) {
                return b.getBundleContext();
            }
        }
        
        throw new NullPointerException("unable to get bundle context for aries blueprint extender");
    }
    
    public URL generateBlueprintModel(Bundle bundle) {
        if (foundBlueprintAnnotation(bundle)) {
            Tblueprint tblueprint = generateBlueprintModel(classes);
            
            if (tblueprint != null) {
                // create the generated blueprint xml file in bundle storage area
                BundleContext extenderContext = getBlueprintExtenderContext();
                File dir = extenderContext.getDataFile(bundle.getSymbolicName() + "/" + bundle.getVersion() + "/");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String blueprintPath = cachePath(bundle, "annotation-generated-blueprint.xml");
                File file = extenderContext.getDataFile(blueprintPath);
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                try {
                    marshallOBRModel(tblueprint, file);
                } catch (JAXBException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                try {
                    return file.toURL();
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
        }
        
        return null;

    }

    private void marshallOBRModel(Tblueprint tblueprint, File blueprintFile) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Tblueprint.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(tblueprint, blueprintFile);
        
    }

    
    private Tblueprint generateBlueprintModel(List<Class> classes) {
        if (classes.isEmpty()) {
            return null;
        }
        
        Tblueprint tblueprint = new Tblueprint();
        List<Object> components = tblueprint.getServiceOrReferenceListOrBean();
        for (Class clazz : classes) {
            if (clazz.isAnnotationPresent(Bean.class)) {
                // @Bean annotation detected
                Tbean tbean = new Tbean();
                tbean.setId(clazz.getSimpleName());
                tbean.setClazz(clazz.getName());
                List<Object> props = tbean.getArgumentOrPropertyOrAny();
                
                Field[] fields = clazz.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].isAnnotationPresent(Inject.class)) {
                       Inject inj = fields[i].getAnnotation(Inject.class);
                       String value = inj.value();
                       Tproperty tp = new Tproperty();
                       tp.setName(fields[i].getName());
                       Tvalue tvalue = new Tvalue();
                       tvalue.setContent(value);
                       tp.setValue(tvalue);
                       props.add(tp);
                    }
                }
                components.add(tbean);
            }
        }
        
        return tblueprint;
        

    }
    
    
    // copy from blueprint extender
    private String cachePath(Bundle bundle, String filePath)
    {
      return bundle.getSymbolicName() + "/" + bundle.getVersion() + "/" + filePath;
    } 
}
