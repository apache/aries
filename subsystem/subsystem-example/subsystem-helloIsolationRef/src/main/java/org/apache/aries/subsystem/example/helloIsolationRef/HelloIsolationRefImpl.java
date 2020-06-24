
package org.apache.aries.subsystem.example.helloIsolationRef;

import org.apache.aries.subsystem.example.helloIsolation.HelloIsolation;

public class HelloIsolationRefImpl implements HelloIsolationRef
{
//IC see: https://issues.apache.org/jira/browse/ARIES-456
  HelloIsolation helloIsolation = null;
  
  public HelloIsolation getHelloIsolationService()
  {
    return this.helloIsolation;
  }

  public void helloRef()
  {
    System.out.println("helloRef from HelloIsolationImpl");
    this.helloIsolation.hello();
  }
  
  public void setHelloIsolationService(HelloIsolation hi) 
  {
    this.helloIsolation = hi;
  }
}
