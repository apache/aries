package org.apache.aries.jndi.legacy.support;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

public class LegacyInitialContextFinder implements InitialContextFactoryBuilder {

	@Override
	public InitialContextFactory createInitialContextFactory(
			Hashtable<?, ?> environment) throws NamingException 
	{
		String icf = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
		if (icf != null) {
			ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
				@Override
				public ClassLoader run() {
					return Thread.currentThread().getContextClassLoader();
				}
			});
			
			try {
				Class<?> icfClass = Class.forName(icf, false, cl);
				if (InitialContextFactory.class.isAssignableFrom(icfClass)) {
					return (InitialContextFactory) icfClass.newInstance();
				}
			} catch (ClassNotFoundException e) {
				// If the ICF doesn't exist this is expected. Should return null so the next builder is queried.
			} catch (InstantiationException e) {
				// If the ICF couldn't be created just ignore and return null.
			} catch (IllegalAccessException e) {
				// If the default constructor is private, just ignore and return null.
			}
		}
		
		return null;
	}

}
