package org.apache.aries.cdi.test.tb152_3;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.osgi.service.cdi.annotations.Bean;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;
import org.osgi.service.cdi.propertytypes.ServiceDescription;

@Bean
@Service
@ServiceDescription("two")
@SingleComponent
public class Two implements BeanService<Boolean> {

	@Override
	public String doSomething() {
		return "";
	}

	@Override
	public Boolean get() {
		return Boolean.TRUE;
	}

}
