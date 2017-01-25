package org.apache.aries.cdi.test.beans;

import java.util.Iterator;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.apache.aries.cdi.test.interfaces.SingletonScoped;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.Service;

@Service(type = {BeanService.class, InstanceServiceReferenceBean.class})
@Singleton
@SuppressWarnings("rawtypes")
public class InstanceServiceReferenceBean implements BeanService<ServiceReference> {

	@Override
	public String doSomething() {
		int count = 0;
		for (Iterator<?> iterator = _instance.iterator();iterator.hasNext();) {
			iterator.next();
			count++;
		}
		return String.valueOf(count);
	}

	@Override
	public ServiceReference get() {
		return _instance.iterator().next();
	}

	@Inject
	@Reference(service = SingletonScoped.class)
	Instance<ServiceReference> _instance;

}
