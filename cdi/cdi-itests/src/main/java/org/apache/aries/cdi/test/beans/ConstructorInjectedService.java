package org.apache.aries.cdi.test.beans;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.service.cdi.annotations.Service;

@Service(type = {ConstructorInjectedService.class, BeanService.class})
@Singleton
public class ConstructorInjectedService implements BeanService<Pojo> {

	@Inject
	public ConstructorInjectedService(PojoImpl pojo) {
		_pojo = pojo;
	}

	@Override
	public String doSomething() {
		return _pojo.foo("CONSTRUCTOR");
	}

	@Override
	public Pojo get() {
		return _pojo;
	}

	private PojoImpl _pojo;

}
