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
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.aries.blueprint.annotation.Arg;
import org.apache.aries.blueprint.annotation.Bean;
import org.apache.aries.blueprint.annotation.Bind;
import org.apache.aries.blueprint.annotation.Blueprint;
import org.apache.aries.blueprint.annotation.Destroy;
import org.apache.aries.blueprint.annotation.Init;
import org.apache.aries.blueprint.annotation.Inject;
import org.apache.aries.blueprint.annotation.Reference;
import org.apache.aries.blueprint.annotation.ReferenceList;
import org.apache.aries.blueprint.annotation.ReferenceListener;
import org.apache.aries.blueprint.annotation.Register;
import org.apache.aries.blueprint.annotation.RegistrationListener;
import org.apache.aries.blueprint.annotation.Service;
import org.apache.aries.blueprint.annotation.ServiceProperty;
import org.apache.aries.blueprint.annotation.Unbind;
import org.apache.aries.blueprint.annotation.Unregister;
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
import org.apache.aries.blueprint.jaxb.TservicePropertyEntry;
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
    private final BundleContext context;

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
            try {
                return file.toURI().toURL();
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

        // we don't trust baf when it comes to returning classes just once (ARIES-654)
        Set<Class> blueprintClasses = new LinkedHashSet<Class>(baf.findAnnotatedClasses(Blueprint.class));
        Set<Class> beanClasses = new HashSet<Class>(baf.findAnnotatedClasses(Bean.class));
        Set<Class> refListenerClasses = new HashSet<Class>(baf.findAnnotatedClasses(ReferenceListener.class));
        Set<Class> regListenerClasses = new HashSet<Class>(baf.findAnnotatedClasses(RegistrationListener.class));
        Map<String, TreferenceListener> reflMap = new HashMap<String, TreferenceListener>();
        Map<String, TregistrationListener> reglMap = new HashMap<String, TregistrationListener>();
        
        Tblueprint tblueprint = new Tblueprint();
        
        
        if (!blueprintClasses.isEmpty()) {
            // use the first annotated blueprint annotation
            Blueprint blueprint = (Blueprint)blueprintClasses.iterator().next().getAnnotation(Blueprint.class);
            tblueprint.setDefaultActivation(blueprint.defaultActivation());
            tblueprint.setDefaultAvailability(blueprint.defaultAvailability());
            tblueprint.setDefaultTimeout(convertToBigInteger(blueprint.defaultTimeout()));
        }

        List<Object> components = tblueprint.getServiceOrReferenceListOrBean();
        
        // try to process classes that have @ReferenceListener or @RegistrationLister first
        // as we want the refl and regl maps populated before processing @Bean annotation.
        for (Class refListener : refListenerClasses) {
            Bean bean = (Bean) refListener.getAnnotation(Bean.class);
                       
            // register the treference with its id
            TreferenceListener tref = generateTrefListener(refListener);
            
            if (bean.id().length() > 0) {
                reflMap.put(bean.id(), tref);
            } else {
                throw new BlueprintAnnotationException("Unable to find the id for the @ReferenceListener annotated class " + refListener.getName());
            }
        }
        
        
        for (Class regListener : regListenerClasses) {
            Bean bean = (Bean) regListener.getAnnotation(Bean.class);
            
            // register the tregistrationListener with its id
            TregistrationListener tref = generateTregListener(regListener);
            
            if (bean.id().length() > 0) {
                reglMap.put(bean.id(), tref);
            } else {
                throw new BlueprintAnnotationException("Unable to find the id for the @RegistrationListener annotated class " + regListener.getName());
            }   
        }
        
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
            
            // process factory ref
            String factoryRef = bean.factoryRef();
            if (factoryRef.length() > 0) {
                tbean.setFactoryRef(factoryRef);
            }
            
            // process factory method
            String factoryMethod = bean.factoryMethod();
            if (factoryMethod.length() > 0) {
                tbean.setFactoryMethod(factoryMethod);
            }
            

            List<Object> props = tbean.getArgumentOrPropertyOrAny();

            // process args 
            Arg[] args = bean.args();
            
            if (args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    Targument targ = createTargument(args[i]);
                    if (targ != null) {
                        props.add(targ);
                    }
                }
            }
            
            Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isAnnotationPresent(Inject.class)) { 
                    if (fields[i].isAnnotationPresent(Reference.class)) {
                        // the field is also annotated with @Reference
                        Reference ref = fields[i].getAnnotation(Reference.class);
                        Treference tref = generateTref(ref, reflMap);
                        components.add(tref);
                    } else if (fields[i].isAnnotationPresent(ReferenceList.class)) {
                        // the field is also annotated with @ReferenceList
                        ReferenceList ref = fields[i].getAnnotation(ReferenceList.class);
                        TreferenceList tref = generateTrefList(ref, reflMap);
                        components.add(tref);
                        
                    } else {
                        Tproperty tp = createTproperty(fields[i].getName(), fields[i].getAnnotation(Inject.class));
                        props.add(tp);
                    }
                }
            }
                    
            // check if the bean also declares init, destroy or inject annotation on methods
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
                } else if (methods[i].isAnnotationPresent(Arg.class)) {
                    Targument targ = createTargument(methods[i].getAnnotation(Arg.class));
                    props.add(targ);     
                }
            }
            
            // check if the bean also declares service
            if (clazz.getAnnotation(Service.class) != null) {
                Tservice tservice = generateTservice(clazz, id, reglMap);
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

        return tblueprint;
    }

    private TreferenceListener generateTrefListener(Class refListener) {
        ReferenceListener rl = (ReferenceListener) refListener.getAnnotation(ReferenceListener.class);
        
        String ref = rl.ref();
        String bind = null;
        String unbind = null;
        
        // also check bind/unbind method
        Method[] methods = refListener.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].isAnnotationPresent(Bind.class)) {
                if (bind == null) {
                    bind = methods[i].getName();
                } else if (!bind.equals(methods[i].getName())) {
                    throw new BlueprintAnnotationException("@Bind annottaed method for reference listener " + refListener.getName() + " are not consistent");       
                }
                continue;
            }
            if (methods[i].isAnnotationPresent(Unbind.class)) {
                if (unbind == null) {
                  unbind = methods[i].getName();
                } else if (!unbind.equals(methods[i].getName())) {
                    throw new BlueprintAnnotationException("@Unbind annotated method for reference listener " + refListener.getName() + " are not consistent");       
                }
                continue;
            }
        }
        
        TreferenceListener trl = new TreferenceListener();
        if (bind != null) {
            trl.setBindMethod(bind);
        }
        if (unbind != null) {
            trl.setUnbindMethod(unbind);
        }
        
        if (ref != null) {
            trl.setRefAttribute(ref);
        }
        
        return trl;
    }
    
    private TregistrationListener generateTregListener(Class regListener) {
        RegistrationListener rl = (RegistrationListener) regListener.getAnnotation(RegistrationListener.class);
        
        String register = null;
        String unregister = null;
        
        // also check bind/unbind method
        Method[] methods = regListener.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].isAnnotationPresent(Register.class)) {
                if (register == null) {
                    register = methods[i].getName();
                } else if (!register.equals(methods[i].getName())) {
                    throw new BlueprintAnnotationException("@Register annottaed method for registration listener " + regListener.getName() + " are not consistent");       
                }
                continue;
            }
            if (methods[i].isAnnotationPresent(Unregister.class)) {
                if (unregister == null) {
                  unregister = methods[i].getName();
                } else if (!unregister.equals(methods[i].getName())) {
                    throw new BlueprintAnnotationException("@Unregister annotated method for registration listener " + regListener.getName() + " are not consistent");       
                }
                continue;
            }
        }
        
        TregistrationListener trl = new TregistrationListener();
        if (register != null) {
            trl.setRegistrationMethod(register);
        }
        if (unregister != null) {
            trl.setUnregistrationMethod(unregister);
        }
        
        return trl;
    }

    private Targument createTargument(Arg arg) {
        String value = arg.value();
        String ref = arg.ref();
        Targument targ = null;
        if (value.length() > 0) {
            targ = new Targument();
            targ.setValueAttribute(value);
        }
        
        if (ref.length() > 0) {
            if (targ == null) {
                targ = new Targument();
            }
            
            targ.setRefAttribute(ref);
        }
        
        // TODO process description, index of Arg annotation
        return targ;
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
    
    private Treference generateTref(Reference ref, Map<String, TreferenceListener> reflMap) {

        String id = ref.id();
        String availability = ref.availability();
        String compName = ref.componentName();
        String desp = ref.description();
        String filter = ref.filter();
        Class<?> serviceInterface = ref.serviceInterface();
        ReferenceListener[] refListeners = ref.referenceListeners();
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
        }
        
        if (timeout > 0) {
            tref.setTimeout(convertToBigInteger(timeout));
        }
        for (ReferenceListener rl : refListeners) {
            String rf = rl.ref();
            TreferenceListener trl = reflMap.get(rf);
            if (trl != null) {
                trl.setRefAttribute(rf);
                tref.getReferenceListener().add(trl);
            } else {
                throw new BlueprintAnnotationException("Unable to find the ReferenceListener ref " + rf);
            }
        }
        
        return tref;
    }
    
    private TreferenceList generateTrefList(ReferenceList ref, Map<String, TreferenceListener> reflMap) {
        String id = ref.id();
        String availability = ref.availability();
        String compName = ref.componentName();
        String desp = ref.description();
        String filter = ref.filter();
        Class<?> serviceInterface = ref.serviceInterface();
        ReferenceListener[] refListeners = ref.referenceListeners();
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
        } 
        
        for (ReferenceListener rl : refListeners) {
            String rf = rl.ref();
            TreferenceListener trl = reflMap.get(rf);
            if (trl != null) {
                trl.setRefAttribute(rf);
                tref.getReferenceListener().add(trl);
            } else {
                throw new BlueprintAnnotationException("Unable to find the ReferenceListener ref " + rf);
            }
        }
        
        return tref;
    }
    
    private Tservice generateTservice(Class clazz, String id, Map<String, TregistrationListener> reglMap) {
        Service service = (Service) clazz.getAnnotation(Service.class);
        Class<?>[] interfaces = service.interfaces();
        int ranking = service.ranking();
        String autoExport = service.autoExport();
        ServiceProperty[] serviceProperties = service.serviceProperties();
        RegistrationListener[] regListeners = service.registerationListeners();
        
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
        
        // process service property.  only key value as string are supported for now
        for (ServiceProperty sp : serviceProperties) {
            if (sp != null) {
                String key = sp.key();
                String value = sp.value();
                if (key.length() > 0 && value.length() > 0) {
                    TservicePropertyEntry tsp = new TservicePropertyEntry();
                    tsp.setKey(key);
                    tsp.setValueAttribute(value);
                    tservice.getServiceProperties().getEntry().add(tsp);
                }
                
            }
        }
        
        for (RegistrationListener regListener : regListeners) {
            String ref = regListener.ref();
            if (ref.length() > 0) {
                TregistrationListener tregListener = reglMap.get(ref);
                tregListener.setRefAttribute(ref);
                tservice.getRegistrationListener().add(tregListener);
                
            } else {
                throw new BlueprintAnnotationException("No ref id for service registration listener " + " for " + clazz.getName());
            }
        }
        
        return tservice;
    }
}
