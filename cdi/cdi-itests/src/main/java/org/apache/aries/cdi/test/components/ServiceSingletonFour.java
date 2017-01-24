package org.apache.aries.cdi.test.components;

import org.apache.aries.cdi.test.interfaces.SingletonScoped;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

@Component(
	property = {Constants.SERVICE_RANKING + ":Integer=4"}
)
public class ServiceSingletonFour implements SingletonScoped<ServiceSingletonFour> {

	@Override
	public ServiceSingletonFour get() {
		return this;
	}

}
