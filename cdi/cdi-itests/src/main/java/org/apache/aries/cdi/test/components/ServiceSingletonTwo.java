package org.apache.aries.cdi.test.components;

import org.apache.aries.cdi.test.interfaces.SingletonScoped;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

@Component(
	property = {Constants.SERVICE_RANKING + ":Integer=2"}
)
public class ServiceSingletonTwo implements SingletonScoped<ServiceSingletonTwo> {

	@Override
	public ServiceSingletonTwo get() {
		return this;
	}

}
