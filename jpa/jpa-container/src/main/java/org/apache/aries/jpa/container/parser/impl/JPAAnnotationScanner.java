/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.container.parser.impl;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

@SuppressWarnings("deprecation")
class JPAAnnotationScanner {
    public static Collection<String> findJPAAnnotatedClasses(Bundle b) {
        BundleWiring bw = b.adapt(BundleWiring.class);
        Collection<String> resources = bw.listResources("/", "*.class", 
            BundleWiring.LISTRESOURCES_LOCAL | BundleWiring.LISTRESOURCES_RECURSE);
        
        Collection<String> classes = new ArrayList<String>(); 
        ClassLoader cl = new TempBundleDelegatingClassLoader(b, JPAAnnotationScanner.class.getClassLoader());
        for(String s : resources) {
          s = s.replace('/', '.').substring(0, s.length() - 6);
          try {
            Class<?> clazz = Class.forName(s, false, cl);
            
            if(clazz.isAnnotationPresent(Entity.class) ||
               clazz.isAnnotationPresent(MappedSuperclass.class) ||
               clazz.isAnnotationPresent(Embeddable.class)) {
              classes.add(s);
            }
            
          } catch (ClassNotFoundException cnfe) {
            
          } catch (NoClassDefFoundError ncdfe) {
            
          }
        }
        return classes;
      }
}
