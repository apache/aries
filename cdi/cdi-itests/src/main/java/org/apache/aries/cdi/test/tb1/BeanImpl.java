package org.apache.aries.cdi.test.tb1;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.apache.aries.cdi.test.interfaces.Pojo;

@Named("beanimpl")
public class BeanImpl implements BeanService<BeanImpl> {

	@Override
	public String doSomething() {
		return pojo.foo("BEAN-IMPL");
	}

	@Override
	public BeanImpl get() {
		return this;
	}

	@Inject
	Pojo pojo;

}
