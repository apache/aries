package org.apache.aries.cdi.test.components;

import org.apache.aries.cdi.test.interfaces.PrototypeScoped;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(
	property = {"key=value"},
	scope = ServiceScope.PROTOTYPE
)
public class ServicePrototypeScope implements PrototypeScoped {

	@Override
	public Object get() {
		return this;
	}

}
