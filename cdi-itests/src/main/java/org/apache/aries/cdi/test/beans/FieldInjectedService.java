package org.apache.aries.cdi.test.beans;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.service.cdi.annotations.Service;

@Service(type = {FieldInjectedService.class, BeanService.class})
@Singleton
public class FieldInjectedService implements BeanService<Pojo> {

	@Override
	public String doSomething() {
		return _pojo.foo("FIELD");
	}

	@Override
	public Pojo get() {
		return _pojo;
	}

	@Inject
	private PojoImpl _pojo;

}
