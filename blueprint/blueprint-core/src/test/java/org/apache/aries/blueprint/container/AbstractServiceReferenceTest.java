package org.apache.aries.blueprint.container;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedList;

import static org.junit.Assert.*;

import org.apache.aries.blueprint.container.AbstractServiceReferenceRecipe.CgLibProxyFactory;
import org.junit.Test;
import org.osgi.service.blueprint.container.ComponentDefinitionException;

public class AbstractServiceReferenceTest {
    @Test
    public void testCglibProxySingleTargetClass() {
        CgLibProxyFactory sut = new CgLibProxyFactory();
        Class<?> result = sut.getTargetClass(new Class<?>[] {ArrayList.class});
        assertEquals(ArrayList.class, result);
    }
    
    @Test
    public void testCglibProxyMultipleTargetClasses() {
        CgLibProxyFactory sut = new CgLibProxyFactory();
        Class<?> result = sut.getTargetClass(new Class<?>[] {AbstractList.class, ArrayList.class});
        assertEquals(ArrayList.class, result);
        
        result = sut.getTargetClass(new Class<?>[] {ArrayList.class, AbstractList.class});
        assertEquals(ArrayList.class, result);
    }
    
    @Test(expected=ComponentDefinitionException.class)
    public void testCglibProxyIncompatibleTargetClasses() {
        CgLibProxyFactory sut = new CgLibProxyFactory();
        sut.getTargetClass(new Class<?>[] {LinkedList.class, ArrayList.class});        
    }
}
