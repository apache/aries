package org.apache.aries.cdi.test.tb4;

import java.util.concurrent.Callable;

public class CallableImpl implements Callable<String> {

	@Override
	public String call() throws Exception {
		return getClass().getName();
	}

}
