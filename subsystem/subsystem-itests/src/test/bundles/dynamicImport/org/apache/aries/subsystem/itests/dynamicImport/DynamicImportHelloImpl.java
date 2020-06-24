package org.apache.aries.subsystem.itests.dynamicImport;

import org.apache.aries.subsystem.itests.hello.api.Hello;

public class DynamicImportHelloImpl implements Hello {

	public DynamicImportHelloImpl() 
	{ 
		System.out.println ("DynamicImportHelloImpl constructed");
	}
	
	@Override
	public String saySomething() {
//IC see: https://issues.apache.org/jira/browse/ARIES-868
		return "Hello, this is something";
	}

}
