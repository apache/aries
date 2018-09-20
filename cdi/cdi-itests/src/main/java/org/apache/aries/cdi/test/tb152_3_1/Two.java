package org.apache.aries.cdi.test.tb152_3_1;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.osgi.service.cdi.annotations.Bean;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;
import org.osgi.service.cdi.propertytypes.ServiceDescription;

@Bean
@Service
@ServiceDescription("two")
@SingleComponent
public class Two implements BeanService<Integer> {

	private volatile String status;

	public Two() {
		status = "CONSTRUCTED";
	}

	@Override
	public String doSomething() {
		return status;
	}

	@Override
	public Integer get() {
		return number;
	}

	@PostConstruct
	void postConstruct() {
		status = "POST_CONSTRUCTED";
	}

	@PreDestroy
	void preDestroy() {
		status = "DESTROYED";
	}

	@Inject
	@Reference
	@ServiceDescription("two")
	Integer number;
}
