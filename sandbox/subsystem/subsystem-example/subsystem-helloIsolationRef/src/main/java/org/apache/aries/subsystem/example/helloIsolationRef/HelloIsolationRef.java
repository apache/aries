
package org.apache.aries.subsystem.example.helloIsolationRef;

import org.apache.aries.subsystem.example.helloIsolation.HelloIsolation;

public interface HelloIsolationRef
{
  public void helloRef();
  
  public HelloIsolation getHelloIsolationService();
}
