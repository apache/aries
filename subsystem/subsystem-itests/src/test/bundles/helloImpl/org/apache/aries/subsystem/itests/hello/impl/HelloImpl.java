package org.apache.aries.subsystem.itests.hello.impl;

import org.apache.aries.subsystem.itests.hello.api.Hello;

public class HelloImpl implements Hello {

	@Override
	public String saySomething() {
		return "something";
	}

}
