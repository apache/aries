package org.apache.aries.cdi.test.tb3;

import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.Service;

@Service(property = "bean=B")
public class ConfigurationBeanB implements BeanService<Callable<int[]>> {

	@Override
	public String doSomething() {
		return (String)config.get("color");
	}

	@Override
	public Callable<int[]> get() {
		return new Callable<int[]>() {
			@Override
			public int[] call() throws Exception {
				return (int[])config.get("ports");
			}
		};
	}

	@Configuration({"$", "configA"})
	@Inject
	@Named("configB")
	Map<String, Object> config;

}
