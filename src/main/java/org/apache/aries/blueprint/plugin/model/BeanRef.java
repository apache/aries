package org.apache.aries.blueprint.plugin.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.springframework.stereotype.Component;

public class BeanRef implements Comparable<BeanRef> {
    public String id;
    public Class<?> clazz;
    public Map<Class<? extends Annotation>, Annotation> qualifiers;
    
    /**
     * 
     * @param clazz interface or implementation class
     */
    public BeanRef(Class<?> clazz) {
        this.clazz = clazz;
        this.qualifiers = new HashMap<Class<? extends Annotation>, Annotation>();
    }
    
    public BeanRef(Class<?> clazz, String id) {
        this(clazz);
        this.id = id;
    }

    public BeanRef(Field field) {
        this(field.getType());
        for (Annotation ann : field.getAnnotations()) {
            if (isQualifier(ann) != null) {
                this.qualifiers.put(ann.annotationType(), ann);
            }
        }
    }

    private javax.inject.Qualifier isQualifier(Annotation ann) {
        return ann.annotationType().getAnnotation(javax.inject.Qualifier.class);
    }

    public static String getBeanName(Class<?> clazz) {
        Component component = clazz.getAnnotation(Component.class);
        Named named = clazz.getAnnotation(Named.class);
        if (component != null && !"".equals(component.value())) {
            return component.value();
        } else if (named != null && !"".equals(named.value())) {
            return named.value();    
        } else {
            String name = clazz.getSimpleName();
            return getBeanNameFromSimpleName(name);
        }
    }

    private static String getBeanNameFromSimpleName(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1, name.length());
    }
    
    public boolean matches(BeanRef template) {
        boolean assignable = template.clazz.isAssignableFrom(this.clazz);
        return assignable && qualifiers.values().containsAll(template.qualifiers.values());
    }

    @Override
    public int compareTo(BeanRef other) {
        return this.id.compareTo(other.id);
    }
    
    @Override
    public String toString() {
        return this.clazz.getSimpleName() + "(" + this.id + ")";
    }
}
