/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.subsystem.example.helloIsolation;

import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

public class HelloIsolationImpl implements HelloIsolation
{

  public void hello()
  {
    System.out.println("hello from HelloIsolationImpl");
  }

  // test java2 security

  public void checkPermission(final Permission permission) throws SecurityException {
      System.out.println("HelloIsolationImpl: enter checkpermission");

    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction() {
        public Object run() throws SecurityException {
          SecurityManager security = System.getSecurityManager();
          if (security != null) {
              System.out.println("HelloIsolationImpl: system manager is not null");

              security.checkPermission(permission);
              return null; 
          }
          System.out.println("HelloIsolationImpl: system manager is still null");

          return null;
        }
      });
    } catch (PrivilegedActionException e) {
      throw (SecurityException) e.getException();
    }
  }
}
