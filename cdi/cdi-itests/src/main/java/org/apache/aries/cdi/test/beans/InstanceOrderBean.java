package org.apache.aries.cdi.test.beans;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.apache.aries.cdi.test.interfaces.SingletonScoped;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.MinCardinality;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.Service;

@Service(type = {BeanService.class, InstanceOrderBean.class})
@Singleton
@SuppressWarnings("rawtypes")
public class InstanceOrderBean implements BeanService<List<ServiceReference>> {

	@Override
	public String doSomething() {
		int count = 0;
		for (Iterator<ServiceReference> iterator = _instance.iterator();iterator.hasNext();) {
			System.out.println(iterator.next());
			count++;
		}
		return String.valueOf(count);
	}

	@Override
	public List<ServiceReference> get() {
		List<ServiceReference> list = new ArrayList<>();
		for (Iterator<ServiceReference> iterator = _instance.iterator();iterator.hasNext();) {
			list.add(iterator.next());
		}
		return list;
	}

	@Inject
	@MinCardinality(3)
	@Reference(service = SingletonScoped.class)
	Instance<ServiceReference> _instance;

}
