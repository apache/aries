/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.extension.jndi;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import org.osgi.service.log.Logger;
import org.osgi.util.promise.Promise;

public class JndiContext implements Context {

	public JndiContext(Logger log, Promise<BeanManager> beanManager) {
		_log = log;
		_beanManager = beanManager;
	}

	@Override
	public Object lookup(Name name) throws NamingException {
		return lookup(name.toString());
	}

	@Override
	public Object lookup(String name) throws NamingException {
		if (name.length() == 0) {
			return new JndiContext(_log, _beanManager);
		}
		if (name.equals("java:comp/BeanManager")) {
			try {
				return _beanManager.timeout(5000).getValue();
			}
			catch (InvocationTargetException | InterruptedException e) {
				_log.error(l -> l.error(e.getMessage(), e));
			}
		}
		throw new NamingException("Could not find " + name);
	}

	@Override
	public void bind(Name name, Object obj) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void bind(String name, Object obj) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void rebind(Name name, Object obj) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void rebind(String name, Object obj) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void unbind(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void unbind(String name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void rename(Name oldName, Name newName) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void rename(String oldName, String newName) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void destroySubcontext(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void destroySubcontext(String name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Context createSubcontext(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Context createSubcontext(String name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Object lookupLink(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Object lookupLink(String name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public NameParser getNameParser(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public NameParser getNameParser(String name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Name composeName(Name name, Name prefix) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public String composeName(String name, String prefix) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Object addToEnvironment(String propName, Object propVal) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Object removeFromEnvironment(String propName) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Hashtable<?, ?> getEnvironment() throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void close() throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public String getNameInNamespace() throws NamingException {
		throw new OperationNotSupportedException();
	}

	private final Logger _log;
	private final Promise<BeanManager> _beanManager;

}