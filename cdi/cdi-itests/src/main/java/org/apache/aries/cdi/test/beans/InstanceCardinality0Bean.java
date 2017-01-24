package org.apache.aries.cdi.test.beans;

import java.util.Iterator;
import java.util.concurrent.Callable;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.osgi.service.cdi.annotations.MinCardinality;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.Service;

@Service(type = {BeanService.class, InstanceCardinality0Bean.class})
@Singleton
public class InstanceCardinality0Bean implements BeanService<Callable<String>> {

	@Override
	public String doSomething() {
		int count = 0;
		for (Iterator<?> iterator = _instance.iterator();iterator.hasNext();) {
			System.out.println(iterator.next());
			count++;
		}
		return String.valueOf(count);
	}

	@Override
	public Callable<String> get() {
		Iterator<Callable<String>> iterator = _instance.iterator();
		return iterator.hasNext() ? iterator.next() : null;
	}

	@Inject
	@MinCardinality(0)
	@Reference
	Instance<Callable<String>> _instance;

}
