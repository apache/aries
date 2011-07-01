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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.util.nls;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * This is a helper class for loading messages for logging and exception messages. It supports translating the message into the
 * default Locale. It works out the calling Bundle and uses it to load any resources. If the resource bundle is of type
 * java.properties then it tries to find the bundle first via the bundle getResources method, then via the getEntry method. If it
 * is of type java.class then it'll use the bundle to load the class.
 */
public final class MessageUtil
{
  /** The resource bundle used to translate messages */
  private final ResourceBundle messages;
  private static final StackFinder finder;

  /** 
   * One of the static methods needs to obtain the caller, so we cheat and use a SecurityManager to get the 
   * classes on the stack.
   */
  private static class StackFinder extends SecurityManager
  {
    @Override
    public Class<?>[] getClassContext()
    {
      return super.getClassContext();
    }
  }
  
  static 
  {
    finder = AccessController.doPrivileged(new PrivilegedAction<StackFinder>() {
      public StackFinder run()
      {
        return new StackFinder();
      }
    });
  }

  private MessageUtil(ResourceBundle b)
  {
    messages = b;
  }

  /**
   * This method translates the message and puts the inserts into the message before returning it.
   * If the message key does not exist then the key itself is returned.
   * 
   * @param key     the message key.
   * @param inserts the inserts into the resolved message.
   * @return the message in the correct language, or the key if the message does not exist.
   */
  public String getMessage(String key, Object ... inserts)
  {
    String message;

    try {
      message = messages.getString(key);

      if (inserts != null && inserts.length > 0) {
        message = MessageFormat.format(message, inserts);
      }
    } catch (MissingResourceException e) {
      message = key;
    }

    return message;
  }

  /**
   * Loads the MessageUtil using the given context. It resolves the Class to an OSGi bundle.
   * 
   * @param context  the bundle this class is in will be used to resolve the base name.
   * @param baseName the resource bundle base name
   * @return the message util instance.
   * @throws MissingResourceException If the resource bundle cannot be located
   */
  public static MessageUtil createMessageUtil(Class<?> context, String baseName)
  {
    return createMessageUtil(FrameworkUtil.getBundle(context), baseName);
  }

  /**
   * This method uses the Bundle associated with the caller of this method.
   * 
   * @param baseName the resource bundle base name
   * @return the message util instance.
   * @throws MissingResourceException If the resource bundle cannot be located
   */
  public static MessageUtil createMessageUtil(String baseName)
  {
    Class<?>[] stack = finder.getClassContext();

    for (Class<?> clazz : stack) {
      if (clazz != MessageUtil.class) {
        return createMessageUtil(clazz, baseName);
      }
    }

    throw new MissingResourceException(org.apache.aries.util.internal.MessageUtil.getMessage("UTIL0014E", baseName), baseName, null);
  }

  /**
   * This method loads the resource bundle backing the MessageUtil from the provided Bundle.
   * 
   * @param b        the bundle to load the resource bundle from
   * @param baseName the resource bundle base name
   * @return the message util instance.
   * @throws MissingResourceException If the resource bundle cannot be located
   */
  public static MessageUtil createMessageUtil(final Bundle b, String baseName)
  {
    ResourceBundle rb;
    
    if (b == null) {
      // if the bundle is null we are probably outside of OSGi, so just use non-OSGi resolve rules.
      rb = ResourceBundle.getBundle(baseName);
    } else {
      // if the bundle is OSGi use OSGi resolve rules as best as Java5 allows
      ClassLoader loader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
        public ClassLoader run() {
            return AriesFrameworkUtil.getClassLoader(b);
        }          
      }); 
      rb = ResourceBundle.getBundle(baseName, Locale.getDefault(), loader);
    }
    
    return new MessageUtil(rb);
  }
}