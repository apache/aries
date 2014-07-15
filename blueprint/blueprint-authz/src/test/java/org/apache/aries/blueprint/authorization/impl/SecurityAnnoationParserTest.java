package org.apache.aries.blueprint.authorization.impl;

import java.lang.annotation.Annotation;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.apache.aries.blueprint.authorization.impl.test.SecuredClass;
import org.junit.Assert;
import org.junit.Test;

public class SecurityAnnoationParserTest {

    private SecurityAnotationParser annParser;
    
    public SecurityAnnoationParserTest() {
        annParser = new SecurityAnotationParser();
    }

    @Test
    public void testAnnotationType() throws NoSuchMethodException, SecurityException {
        Assert.assertTrue(getEffective("admin") instanceof RolesAllowed);
        Assert.assertTrue(getEffective("user") instanceof RolesAllowed);
        Assert.assertTrue(getEffective("anon") instanceof PermitAll);
        Assert.assertTrue(getEffective("closed") instanceof DenyAll);
    }
    
    @Test
    public void testRoles() throws NoSuchMethodException, SecurityException {
        Assert.assertArrayEquals(new String[]{"admin"}, getRoles("admin"));
        Assert.assertArrayEquals(new String[]{"user"}, getRoles("user"));
    }

    private Annotation getEffective(String methodName) throws NoSuchMethodException {
        return annParser.getEffectiveAnnotation(SecuredClass.class.getMethod(methodName));
    }
    
    private String[] getRoles(String methodName) throws NoSuchMethodException {
        Annotation ann = getEffective(methodName);
        Assert.assertTrue(ann instanceof RolesAllowed);
        return ((RolesAllowed)ann).value();
    }
}
