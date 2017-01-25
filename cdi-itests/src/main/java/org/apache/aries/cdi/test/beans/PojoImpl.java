package org.apache.aries.cdi.test.beans;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.apache.aries.cdi.test.interfaces.Pojo;

@ApplicationScoped
public class PojoImpl implements Pojo {

	@Override
	public String foo(String fooInput) {
		_counter.incrementAndGet();
		return "PREFIX" + fooInput;
	}

	@Override
	public int getCount() {
		return _counter.get();
	}

	private AtomicInteger _counter = new AtomicInteger();

}
