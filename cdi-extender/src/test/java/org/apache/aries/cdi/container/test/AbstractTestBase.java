package org.apache.aries.cdi.container.test;

import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class AbstractTestBase {

	@Rule
	public TestWatcher watchman= new TestWatcher() {
		@Override
		protected void failed(Throwable e, Description description) {
			System.out.printf("--------- TEST: %s#%s [%s]%n", description.getTestClass(), description.getMethodName(), "FAILED");
		}

		@Override
		protected void succeeded(Description description) {
			System.out.printf("--------- TEST: %s#%s [%s]%n", description.getTestClass(), description.getMethodName(), "PASSED");
		}
	};

}
