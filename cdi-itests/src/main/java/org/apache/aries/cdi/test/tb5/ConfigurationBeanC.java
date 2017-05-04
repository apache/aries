package org.apache.aries.cdi.test.tb5;

import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.Service;

@Service(property = "bean=C")
public class ConfigurationBeanC implements BeanService<Callable<int[]>> {

	@Override
	public String doSomething() {
		return config.color();
	}

	@Override
	public Callable<int[]> get() {
		return new Callable<int[]>() {
			@Override
			public int[] call() throws Exception {
				return config.ports();
			}
		};
	}

	@Configuration(required = false, value = "foo.bar")
	@Inject
	@Named("configC")
	Config config;

}
