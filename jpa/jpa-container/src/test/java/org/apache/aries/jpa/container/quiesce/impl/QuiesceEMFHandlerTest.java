package org.apache.aries.jpa.container.quiesce.impl;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.junit.Test;

public class QuiesceEMFHandlerTest {

    /** 
     * Tests that we get the real exception thrown by the delegate
     */
    @Test(expected=PersistenceException.class)
    public void testPersistenceExceptionNotWrapped() throws NoSuchMethodException, SecurityException, Throwable {
        EntityManagerFactory delegate = mock(EntityManagerFactory.class);
        doThrow(new PersistenceException()).when(delegate).close();
        QuiesceEMFHandler emfHandler = new QuiesceEMFHandler(delegate, "");
        Method method = EntityManagerFactory.class.getMethod("close", (Class<?>[])null);
        emfHandler.invoke(delegate, method, null);
    }

}
