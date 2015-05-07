package org.osgi.service.async.delegate;

import java.lang.reflect.Method;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.util.promise.Promise;

/**
 * The AsyncDelegate, as defined in OSGi RFC 204
 * https://github.com/osgi/design/tree/master/rfcs/rfc0206
 */
@ConsumerType
public interface AsyncDelegate {
	/**
	 * Asynchronously call a method
	 * 
	 * @param m the method
	 * @param args the arguments
	 * 
	 * @return A promise, or <code>null</code> if the method is not supported
     *
	 * @throws Exception
     */
	Promise<?> async(Method m, Object[] args) throws Exception;

	/**
     * Asynchronously call a method
	 * 
     * @param m the method
     * @param args the arguments
	 * 
	 * @return <code>true<code> if accepted, or <code>false</code> otherwise.
	 * @throws Exception 
	 */
	boolean execute(Method m, Object[] args) throws Exception;
}
