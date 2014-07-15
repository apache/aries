package org.apache.aries.blueprint.authorization.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

/**
 * Evaluates JEE security annotations 
 * @see PermitAll
 * @see DenyAll
 * @see RolesAllowed
 */
class SecurityAnotationParser {

    /**
     * Get the effective annotation regarding method annotations override class annotations.
     * DenyAll has highest priority then RolesAllowed and in the end PermitAll. 
     * So the most restrictive annotation is pereferred.
     * 
     * @param m Method to check
     * @return effective annotation (either DenyAll, PermitAll or RolesAllowed)
     */
    Annotation getEffectiveAnnotation(Method m) {
        Annotation classLevel = getAuthAnnotation(m.getDeclaringClass());
        Annotation methodLevel = getAuthAnnotation(m);
        return (methodLevel != null) ? methodLevel : classLevel;
    }

    private Annotation getAuthAnnotation(AnnotatedElement element) {
        Annotation ann = null;
        ann = element.getAnnotation(DenyAll.class);
        if (ann == null) {
            ann = element.getAnnotation(RolesAllowed.class);
        }
        if (ann == null) {
            ann = element.getAnnotation(PermitAll.class);
        }
        return ann;
    }

}
