package org.apache.aries.subsystem.itests.blueprint;

import org.apache.aries.subsystem.itests.hello.api.Hello;

public class BPHelloImpl implements Hello 
{
	private String _message;
	public void setMessage(String msg) 
	{
		_message = msg;
	}
	
	@Override
	public String saySomething() 
	{
		return _message; 
	}
}
