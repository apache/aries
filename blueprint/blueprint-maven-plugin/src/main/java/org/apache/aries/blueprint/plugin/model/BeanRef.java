package org.apache.aries.blueprint.plugin.model;

import java.lang.reflect.Field;

import javax.inject.Named;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

public class BeanRef implements Comparable<BeanRef> {
    public String id;
    public Class<?> clazz;
    
    /**
     * 
     * @param clazz interface or implementation class
     */
    public BeanRef(Class<?> clazz) {
        this.clazz = clazz;
    }
    
    public BeanRef(Class<?> type, String id) {
        this.clazz = type;
        this.id = id;
    }

    public BeanRef(Field field) {
        this(field.getType(), getDestinationId(field));
    }

    private static String getDestinationId(Field field) {
        Named named = field.getAnnotation(Named.class);
        if (named != null) {
            return named.value();
        }
        Qualifier qualifier = field.getAnnotation(Qualifier.class);
        if (qualifier != null) {
            return qualifier.value();
        }
        return null;
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
        return assignable && ((template.id == null) || id.equals(template.id));
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
