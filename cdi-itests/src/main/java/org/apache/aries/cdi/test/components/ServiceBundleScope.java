package org.apache.aries.cdi.test.components;

import org.apache.aries.cdi.test.interfaces.BundleScoped;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(
	property = {
		"fee.fi=fee",
		"fo.fum:Integer=23",
		"complex.enough.key=fum",
		"key=value",
		"simple.annotation=blah"
	},
	scope = ServiceScope.BUNDLE
)
public class ServiceBundleScope implements BundleScoped {

	@Override
	public Object get() {
		return this;
	}

}
