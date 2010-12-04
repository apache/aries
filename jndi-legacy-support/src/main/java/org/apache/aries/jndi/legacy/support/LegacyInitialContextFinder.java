package org.apache.aries.jndi.legacy.support;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

/**
 * Some OSGi based server runtimes, such as jetty OSGi and virgo rely on the thread context classloader
 * to make their JNDI InitialContextFactory's available in OSGi, rather than relying on the OSGi JNDI spec.
 * This is a little bizare, but perhaps is just a point in time statement. In any case to support them
 * using Aries JNDI we have this ICFB which uses the Thread context classloader. We don't ship it in the
 * jndi uber bundle because it is only for these runtimes which haven't caught up with the latest OSGi specs.
 * Normally we want to enourage the use of the OSGi spec, but this is a backstop for those wanting to use
 * Aries JNDI and one of these runtimes.
 *
 */
public class LegacyInitialContextFinder implements InitialContextFactoryBuilder {

	public InitialContextFactory createInitialContextFactory(
			Hashtable<?, ?> environment) throws NamingException 
	{
		String icf = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
		if (icf != null) {
			ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
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
