package org.apache.aries.cdi.test.tb2;

import org.apache.aries.cdi.test.interfaces.Pojo;

public class PojoImpl implements Pojo {

	@Override
	public String foo(String fooInput) {
		return "POJO-IMPL" + fooInput;
	}

	@Override
	public int getCount() {
		return 1;
	}

}
