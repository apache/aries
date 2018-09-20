package org.apache.aries.cdi.test.tb152_3;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.spi.Context;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.osgi.service.cdi.annotations.Bean;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.propertytypes.ServiceDescription;

@ApplicationScoped
@Bean
@Service
@ServiceDescription("one")
public class One implements BeanService<Context> {

	private Context _context;

	void onComponent(@Observes @Initialized(ComponentScoped.class) Object obj, BeanManager bm) {
		_context = bm.getContext(ComponentScoped.class);
	}

	@Override
	public String doSomething() {
		return _context.toString();
	}

	@Override
	public Context get() {
		return _context;
	}

}
