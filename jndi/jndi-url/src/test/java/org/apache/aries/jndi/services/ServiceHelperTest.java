package org.apache.aries.jndi.services;

import java.util.Collection;

import org.apache.aries.jndi.services.ServiceHelper;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ServiceHelperTest {

	interface A {};
	interface B extends A{};
	interface C {};
	interface D extends A, C{};
	
	@Test
	public void testGetAllInterfaces() throws Exception {
		
		Class<?>[] classes = { B.class, D.class };
		Collection<Class<?>> cx = ServiceHelper.getAllInterfaces(classes);
		
		assertTrue (cx.contains(A.class));
		assertTrue (cx.contains(B.class));
		assertTrue (cx.contains(C.class));
		assertTrue (cx.contains(D.class));
		assertTrue (cx.size() == 4);
		
	}
}
