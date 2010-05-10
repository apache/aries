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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.aries.blueprint.annotation.Bean;
import org.apache.aries.blueprint.annotation.Blueprint;
import org.apache.aries.blueprint.annotation.Destroy;
import org.apache.aries.blueprint.annotation.FactoryMethod;
import org.apache.aries.blueprint.annotation.Init;
import org.apache.aries.blueprint.annotation.Inject;
import org.apache.aries.blueprint.annotation.Reference;
import org.apache.aries.blueprint.annotation.ReferenceList;
import org.apache.aries.blueprint.annotation.ReferenceListener;
import org.apache.aries.blueprint.annotation.RegistrationListener;
import org.apache.aries.blueprint.annotation.Service;
import org.apache.aries.blueprint.annotation.service.BlueprintAnnotationScanner;
import org.apache.aries.blueprint.jaxb.Targument;
import org.apache.aries.blueprint.jaxb.Tbean;
import org.apache.aries.blueprint.jaxb.Tblueprint;
import org.apache.aries.blueprint.jaxb.Tdescription;
import org.apache.aries.blueprint.jaxb.Tinterfaces;
import org.apache.aries.blueprint.jaxb.Tproperty;
import org.apache.aries.blueprint.jaxb.Treference;
import org.apache.aries.blueprint.jaxb.TreferenceList;
import org.apache.aries.blueprint.jaxb.TreferenceListener;
import org.apache.aries.blueprint.jaxb.TregistrationListener;
import org.apache.aries.blueprint.jaxb.Tservice;
import org.apache.aries.blueprint.jaxb.TtypeConverters;
import org.apache.aries.blueprint.jaxb.Tvalue;
import org.apache.xbean.finder.BundleAnnotationFinder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.packageadmin.PackageAdmin;

public class BlueprintAnnotationScannerImpl implements
        BlueprintAnnotationScanner {
    private BundleContext context;

    public BlueprintAnnotationScannerImpl(BundleContext bc) {
        this.context = bc;
    }

    private BundleContext getBlueprintExtenderContext() {
        Bundle[] bundles = this.context.getBundles();
        for (Bundle b : bundles) {
            if (b.getSymbolicName().equals("org.apache.aries.blueprint.core")) {
                return b.getBundleContext();
            }
        }

        return null;
    }

    private BundleAnnotationFinder createBundleAnnotationFinder(Bundle bundle) {
        ServiceReference sr = this.context.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin pa = (PackageAdmin) this.context.getService(sr);
        BundleAnnotationFinder baf = null;
        try {
            baf = new BundleAnnotationFinder(pa, bundle);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        this.context.ungetService(sr);
        
        return baf;
    }
    
    public URL createBlueprintModel(Bundle bundle) {

        Tblueprint tblueprint = generateBlueprintModel(bundle);

        if (tblueprint != null) {
            // create the generated blueprint xml file in bundle storage
            // area
            BundleContext ctx = getBlueprintExtenderContext();

            if (ctx == null) {
                // blueprint extender doesn't exist, let' still generate the
                // bundle, using the bundle's bundle context
                ctx = bundle.getBundleContext();
            }

            File dir = ctx.getDataFile(bundle.getSymbolicName() + "/"
                    + bundle.getVersion() + "/");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String blueprintPath = cachePath(bundle,
                    "annotation-generated-blueprint.xml");
            File file = ctx.getDataFile(blueprintPath);
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
            
            System.out.println("generated annotation xml is located " + file.getAbsolutePath());
            try {
                return file.toURL();
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return null;

    }

    private void marshallOBRModel(Tblueprint tblueprint, File blueprintFile)
            throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Tblueprint.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(tblueprint, blueprintFile);

    }

    private Tblueprint generateBlueprintModel(Bundle bundle) {
        BundleAnnotationFinder baf = createBundleAnnotationFinder(bundle);

        List<Class> blueprintClasses = baf.findAnnotatedClasses(Blueprint.class);
        List<Class> beanClasses = baf.findAnnotatedClasses(Bean.class);
        List<Class> refClasses = baf.findAnnotatedClasses(Reference.class);
        List<Class> refListClasses = baf.findAnnotatedClasses(ReferenceList.class);
        
        Tblueprint tblueprint = new Tblueprint();
        
        
        if (!blueprintClasses.isEmpty()) {
            // use the first annotated blueprint annotation
            Blueprint blueprint = (Blueprint)blueprintClasses.get(0).getAnnotation(Blueprint.class);
            tblueprint.setDefaultActivation(blueprint.defaultActivation());
            tblueprint.setDefaultAvailability(blueprint.defaultAvailability());
            tblueprint.setDefaultTimeout(convertToBigInteger(blueprint.defaultTimeout()));
        }

        List<Object> components = tblueprint.getServiceOrReferenceListOrBean();
        
        for (Class clazz : beanClasses) {
            // @Bean annotation detected
            Bean bean = (Bean)clazz.getAnnotation(Bean.class);
            Tbean tbean = new Tbean();
            
            // process depends-on property
            String[] dependsOn = bean.dependsOn();
            if (!containsValid(dependsOn)) {
                tbean.setDependsOn(null);
            } else {
                List<String> dons = Arrays.asList(dependsOn);
                tbean.setDependsOn(dons);
            }
            
            // process id property
            String id = bean.id();
            if (id.length() > 0) {
                tbean.setId(id);
            } else {
                // should we auto generate an id, based on the class name?
                tbean.setId(clazz.getSimpleName());
            }

            // process the clazz property
            tbean.setClazz(clazz.getName());
            
            // process activation
            String activation = bean.activation();
            if (activation.length() > 0) {
                if (activation.equalsIgnoreCase("eager") || activation.equalsIgnoreCase("lazy")) {
                    tbean.setActivation(bean.activation());
                } else {
                    throw new BlueprintAnnotationException("Invalid bean activation value " + activation + " for " + clazz.getName());
                }
            }
            
            // process description
            if (bean.description().length() > 0) {
                Tdescription desp = new Tdescription();
                desp.getContent().add(bean.description());
                tbean.setDescription(desp);
            }
            
            // process scope
            String scope = bean.scope();
            if (scope.length() > 0) {
                if (scope.equalsIgnoreCase("singleton") || scope.equalsIgnoreCase("prototype")) {
                    tbean.setScope(scope);
                } else {
                    throw new BlueprintAnnotationException("Invalid bean scope value " + scope + " for " + clazz.getName());
                }
            }
            
            List<Object> props = tbean.getArgumentOrPropertyOrAny();

            Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isAnnotationPresent(Inject.class)) {          
                    Tproperty tp = createTproperty(fields[i].getName(), fields[i].getAnnotation(Inject.class));
                    props.add(tp);
                }
            }
                    
            // check if the bean also declares init, destroy or inject or factoryMethod annotation on methods
            Method[] methods = clazz.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].isAnnotationPresent(Init.class)) {
                    tbean.setInitMethod(methods[i].getName());
                } else if (methods[i].isAnnotationPresent(Destroy.class)) {
                    tbean.setDestroyMethod(methods[i].getName());
                } else if (methods[i].isAnnotationPresent(Inject.class)) {
                    String propertyName = convertFromMethodName(methods[i].getName());
                    Tproperty tp = createTproperty(propertyName, methods[i].getAnnotation(Inject.class));
                    props.add(tp);
                } else if (methods[i].isAnnotationPresent(FactoryMethod.class)) {
                    FactoryMethod fm = (FactoryMethod)methods[i].getAnnotation(FactoryMethod.class);
                    tbean.setFactoryMethod(methods[i].getName());
                    String[] values = fm.values();
                    for (int j = 0; j < fm.values().length; j++) {
                        Targument targument = new Targument();
                        targument.setValueAttribute(fm.values()[j]);
                        props.add(targument);
                    }
                    
                    
                    
                }
                
            }
            
            // check if the bean also declared inject annotation on constructors
            Constructor[] constructors = clazz.getConstructors();
            for (int i = 0; i < constructors.length; i++) {
                if (constructors[i].isAnnotationPresent(Inject.class)) {
                    Inject inj = (Inject)constructors[i].getAnnotation(Inject.class);
                    
                    if (inj.value().length() > 0) {
                        Targument targument = new Targument();
                        targument.setValueAttribute(inj.value());
                        props.add(targument);
                    } else if (inj.values().length > 0) {
                        for (int j = 0; j < inj.values().length; j++) {
                            Targument targument = new Targument();
                            targument.setValueAttribute(inj.values()[j]);
                            props.add(targument);
                        }
                    }
                } 
            }
            
            // check if the bean also declares service
            if (clazz.getAnnotation(Service.class) != null) {
                Tservice tservice = generateTservice(clazz, id);
                components.add(tservice);
            }
            
            // check if the clazz implement Converter, if so, it is Converter
            boolean isConverter = isConverter(clazz);
            if (isConverter) {
                TtypeConverters converters = tblueprint.getTypeConverters(); 
                List<Object> objects = converters.getBeanOrReferenceOrRef();
                objects.add(tbean);
            } else {
                components.add(tbean);
            }
        }
        
        for (Class refClass : refClasses) {
            Treference tref = generateTref(refClass);
            components.add(tref);
        }

        for (Class refListClass : refListClasses) {
            TreferenceList trefList = generateTrefList(refListClass);
            components.add(trefList);
        }

        return tblueprint;
    }

    private String convertFromMethodName(String name) {
        if (name.length() > 3) {
            name = name.substring(3);
        } else {
            throw new BlueprintAnnotationException("The annotated method name " + name + " is invalid");
        }
        String firstChar = name.substring(0, 1).toLowerCase();
        
        if (name.length() == 1) {
            return firstChar;
        } else {
            return firstChar + name.substring(1);
        }
    }

    /**
     * @param nm    method or field name
     * @param inj   inject annotation
     * @return
     */
    private Tproperty createTproperty(String nm, Inject inj) {
        String value = inj.value();
        String ref = inj.ref();
        String name = inj.name();
        String desp = inj.description();
                         
        Tproperty tp = new Tproperty();
        if (value.length() > 0) {
            Tvalue tvalue = new Tvalue();
            tvalue.setContent(value);
            tp.setValue(tvalue);
        }
        
        if (ref.length() > 0) {
            tp.setRefAttribute(ref);
        }
        
        if (name.length() > 0) {
            tp.setName(name);
        } else {
            tp.setName(nm);
        }
        
        if (desp.length() > 0) {
            Tdescription tdesp = new Tdescription();
            tdesp.getContent().add(desp);
            tp.setDescription(tdesp);
            
        }
        
        return tp;
    }

    private boolean isConverter(Class clazz) {
        Class[] classes = clazz.getInterfaces();
        for (int i = 0; i < classes.length; i++) {
            if (classes[i].getName().equals(Converter.class.getName())) {
                return true;
            }
        
        }
        return false;

    }
   
    private BigInteger convertToBigInteger(int timeout) {
        return BigInteger.valueOf(timeout * 1000);
    }

    private boolean containsValid(String[] dependsOn) {
        for (int i = 0; i < dependsOn.length; i++) {
            if (dependsOn[i].length() != 0) {
                return true;
            }
        }
        return false;
    }

    // copy from blueprint extender
    private String cachePath(Bundle bundle, String filePath) {
        return bundle.getSymbolicName() + "/" + bundle.getVersion() + "/"
                + filePath;
    }
    
    private Treference generateTref(Class refClass) {
        // @Reference annotation detected
        Reference ref = (Reference)refClass.getAnnotation(Reference.class);
        
        String id = ref.id();
        String availability = ref.availability();
        String compName = ref.componentName();
        String desp = ref.description();
        String filter = ref.filter();
        Class<?> serviceInterface = ref.serviceInterface();
        ReferenceListener[] refListeners = ref.referenceListener();
        int timeout = ref.timeout();
        Treference tref = new Treference();
        
        // can not think of configuring depends on for reference
        tref.setDependsOn(null);
        
        if (id.length() > 0) {
            tref.setId(id);
        }
        
        if (availability.length() > 0) {
            tref.setAvailability(availability);
        }
        if (compName.length() > 0) {
            tref.setComponentName(compName);
        }
        if (desp.length() > 0) {
            Tdescription value = new Tdescription();
            value.getContent().add(desp);
            tref.setDescription(value);
        }
        if (filter.length() > 0) {
            tref.setFilter(filter);
        }
        if (serviceInterface != Object.class) {
            tref.setInterface(serviceInterface.getName());
        } else {
            boolean isInterface =  refClass.isInterface();
            if (isInterface) {
                tref.setInterface(refClass.getName());
            } else {
                // should we throw an exception?  
            }
        }
        
        if (timeout > 0) {
            tref.setTimeout(convertToBigInteger(timeout));
        }
        for (ReferenceListener rl : refListeners) {
            TreferenceListener trl = new TreferenceListener();
            String rf = rl.ref();
            String bindMethod = rl.bind();
            String unbindMethod = rl.unbind();
            trl.setRefAttribute(rf);
            trl.setBindMethod(bindMethod);
            trl.setUnbindMethod(unbindMethod);
            tref.getReferenceListener().add(trl);
        }
        
        return tref;
    }
    
    private TreferenceList generateTrefList(Class refClass) {
        // @ReferenceList annotation detected
        ReferenceList ref = (ReferenceList)refClass.getAnnotation(ReferenceList.class);
        
        String id = ref.id();
        String availability = ref.availability();
        String compName = ref.componentName();
        String desp = ref.description();
        String filter = ref.filter();
        Class<?> serviceInterface = ref.serviceInterface();
        ReferenceListener[] refListeners = ref.referenceListener();
        TreferenceList tref = new TreferenceList();
        
        // can not think of configuring depends on for referencelist
        tref.setDependsOn(null);
        
        if (id.length() > 0) {
            tref.setId(id);
        }
        
        if (availability.length() > 0) {
            tref.setAvailability(availability);
        }
        if (compName.length() > 0) {
            tref.setComponentName(compName);
        }
        if (desp.length() > 0) {
            Tdescription value = new Tdescription();
            value.getContent().add(desp);
            tref.setDescription(value);
        }
        if (filter.length() > 0) {
            tref.setFilter(filter);
        }
        if (serviceInterface  != Object.class) {
            tref.setInterface(serviceInterface.getName());
        } else {
            boolean isInterface =  refClass.isInterface();
            if (isInterface) {
                tref.setInterface(refClass.getName());
            } else {
                // should we throw an exception?  
            }
        }
        for (ReferenceListener rl : refListeners) {
            TreferenceListener trl = new TreferenceListener();
            String rf = rl.ref();
            String bindMethod = rl.bind();
            String unbindMethod = rl.unbind();
            trl.setRefAttribute(rf);
            trl.setBindMethod(bindMethod);
            trl.setUnbindMethod(unbindMethod);
            tref.getReferenceListener().add(trl);
        }
        
        return tref;
    }
    
    private Tservice generateTservice(Class clazz, String id) {
        Service service = (Service) clazz.getAnnotation(Service.class);
        Class<?>[] interfaces = service.interfaces();
        int ranking = service.ranking();
        String autoExport = service.autoExport();
        RegistrationListener[] regListeners = service.registerationListener();
        
        Tservice tservice = new Tservice();
        
        // can not think of configuring depends on for service
        tservice.setDependsOn(null);
        
        // use the bean id as the ref value since we are exposing service for the bean
        tservice.setRefAttribute(id);
        
        if (autoExport.length() > 0) {
            tservice.setAutoExport(autoExport);
        }
        if (ranking > 0) {
            tservice.setRanking(ranking);
        }
        for (Class<?> interf : interfaces) {
            Tinterfaces tInterfaces = new Tinterfaces();
            if (interf != null) {
                tInterfaces.getValue().add(interf.getName());
            }
            tservice.setInterfaces(tInterfaces);
        }
        
        for (RegistrationListener regListener : regListeners) {
            String regListenerId = regListener.id();
            if (regListenerId.length() > 0) {
                TregistrationListener tregListener = new TregistrationListener();
                tregListener.setRefAttribute(regListenerId);
                tregListener.setRegistrationMethod(regListener.register());
                tregListener.setUnregistrationMethod(regListener.unregister());
                tservice.getRegistrationListener().add(tregListener);
                
            } else {
                throw new BlueprintAnnotationException("No ref id for service registration listener " + " for " + clazz.getName());
            }
        }
        
        return tservice;
    }
}
