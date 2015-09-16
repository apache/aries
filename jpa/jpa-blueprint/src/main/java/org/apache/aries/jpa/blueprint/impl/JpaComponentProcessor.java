package org.apache.aries.jpa.blueprint.impl;

import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_NAME;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ComponentDefinitionRegistryProcessor;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableReferenceMetadata;
import org.apache.aries.jpa.blueprint.supplier.impl.ServiceProxy;
import org.apache.aries.jpa.supplier.EmSupplier;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.coordinator.Coordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaComponentProcessor implements ComponentDefinitionRegistryProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaComponentProcessor.class);
    private AnnotationScanner annotationScanner;
    private ParserContext pc;

    public JpaComponentProcessor() {
        annotationScanner = new AnnotationScanner();
    }
    
    public void setPc(ParserContext pc) {
        this.pc = pc;
    }

    @Override
    public void process(ComponentDefinitionRegistry cdr) {
        PassThroughMetadata bundleMeta = (PassThroughMetadata)cdr.getComponentDefinition("blueprintBundle");
        Bundle bundle = (Bundle)bundleMeta.getObject();
        
        Set<String> components = new HashSet<>(cdr.getComponentDefinitionNames());
        for (String component : components) {
            ComponentMetadata compDef = cdr.getComponentDefinition(component);
            if (compDef instanceof MutableBeanMetadata && !((MutableBeanMetadata)compDef).isProcessor()) {
                handleComponent((MutableBeanMetadata)compDef, bundle, cdr);
            }
        }
        System.out.println(cdr.getComponentDefinitionNames());
    }

    private void handleComponent(MutableBeanMetadata compDef, Bundle bundle, ComponentDefinitionRegistry cdr) {
        if (compDef.getClassName() == null) {
            LOGGER.warn("No classname for " + compDef.getId());
            return;
        }
        String compName = compDef.getId();
        Class<?> compClass;
        try {
            compClass = bundle.loadClass(compDef.getClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Bean class not found " + compDef.getClassName());
        }
        BundleContext context = bundle.getBundleContext();
        compDef.setFieldInjection(true);
        List<AccessibleObject> jpaAnnotatedMembers = annotationScanner.getJpaAnnotatedMembers(compClass);
        for (AccessibleObject member : jpaAnnotatedMembers) {
            member.setAccessible(true);
            String propName = getName(member);

            PersistenceContext pcAnn = member.getAnnotation(PersistenceContext.class);
            if (pcAnn != null) {
                LOGGER.info("Adding jpa interceptor for bean {}, prop {} with class {}", compName, propName, compClass);
                Class<?> iface = getType(member);
                if (iface != null) {
                    MutableRefMetadata emRef = getServiceRef(cdr, pcAnn.unitName(), iface);
                    compDef.addProperty(propName, emRef);

                    Interceptor interceptor = createInterceptor(context, pcAnn);
                    cdr.registerInterceptorWithComponent(compDef, interceptor);

                }
            }

            PersistenceUnit puAnn = member.getAnnotation(PersistenceUnit.class);
            if (puAnn != null) {
                LOGGER.info("Adding emf proxy for bean {}, prop {} with class {}", compName, propName, compClass);
                MutableRefMetadata emfRef = getServiceRef(cdr, puAnn.unitName(), EntityManagerFactory.class);
                compDef.addProperty(propName, emfRef);
            }
            
        }
    }

    private MutableRefMetadata getServiceRef(ComponentDefinitionRegistry cdr, String unitName, Class<?> iface) {
        ComponentMetadata serviceRef = cdr.getComponentDefinition(getId(unitName, iface));
        if (serviceRef == null)  {
            serviceRef = createServiceRef(unitName, iface);
            cdr.registerComponentDefinition(serviceRef);
        } else {
            LOGGER.info("Using already registered ref " + serviceRef.getId());
        }
        MutableRefMetadata ref = pc.createMetadata(MutableRefMetadata.class);
        ref.setComponentId(serviceRef.getId());
        return ref;
    }



    private Interceptor createInterceptor(BundleContext context, PersistenceContext pcAnn) {
        String filter = getFilter(EmSupplier.class, pcAnn.unitName());
        EmSupplier supplierProxy = ServiceProxy.create(context, EmSupplier.class, filter);
        Coordinator coordinator = ServiceProxy.create(context, Coordinator.class);
        Interceptor interceptor = new JpaInterceptor(supplierProxy, coordinator);
        return interceptor;
    }

    private String getName(AccessibleObject member) {
        if (member instanceof Field) {
            return ((Field)member).getName();
        } else if (member instanceof Method) {
            Method method = (Method)member;
            String name = method.getName();
            if (!name.startsWith("set")) {
                return null;
            }
            return name. substring(3, 4).toLowerCase() + name.substring(4);
        }
        return null;
    }
    
    private Class<?> getType(AccessibleObject member) {
        if (member instanceof Field) {
            return ((Field)member).getType();
        } else if (member instanceof Method) {
            Method method = (Method)member;
            return method.getParameterTypes()[0];
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    ComponentMetadata createServiceRef(String unitName, Class<?> iface) {
        final MutableReferenceMetadata refMeta = pc.createMetadata(MutableReferenceMetadata.class);
        refMeta.setActivation(getDefaultActivation(pc));
        refMeta.setAvailability(ReferenceMetadata.AVAILABILITY_MANDATORY);
        refMeta.setRuntimeInterface(iface);
        refMeta.setFilter(String.format("(%s=%s)", JPA_UNIT_NAME, unitName));
        refMeta.setTimeout(Integer.parseInt(pc.getDefaultTimeout()));
        refMeta.setDependsOn((List<String>)Collections.EMPTY_LIST);
        refMeta.setId(getId(unitName, iface));
        return refMeta;
    }
    
    public String getId(String unitName, Class<?> iface) {
        return unitName + "_" + iface.getSimpleName();
    }
    
    private int getDefaultActivation(ParserContext ctx) {
        return "ACTIVATION_EAGER".equalsIgnoreCase(ctx.getDefaultActivation())
            ? ReferenceMetadata.ACTIVATION_EAGER : ReferenceMetadata.ACTIVATION_LAZY;
    }
    
    private String getFilter(Class<?> clazz, String unitName) {
        return String.format("(&(objectClass=%s)(%s=%s))", clazz.getName(), JPA_UNIT_NAME, unitName);
    }
}
