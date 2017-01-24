package org.apache.aries.cdi.test.components;

import org.apache.aries.cdi.test.interfaces.SingletonScoped;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

@Component(
	property = {Constants.SERVICE_RANKING + ":Integer=-1"}
)
public class ServiceSingletonThree implements SingletonScoped<ServiceSingletonThree> {

	@Override
	public ServiceSingletonThree get() {
		return this;
	}

}
