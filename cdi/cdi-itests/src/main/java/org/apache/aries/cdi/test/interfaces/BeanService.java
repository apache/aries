package org.apache.aries.cdi.test.interfaces;

public interface BeanService<T> {

	public String doSomething();

	public T get();

}
