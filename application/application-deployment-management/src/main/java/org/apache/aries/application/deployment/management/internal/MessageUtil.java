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

package org.apache.aries.application.deployment.management.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MessageUtil
{
  /** The resource bundle for blueprint messages */
  private final static ResourceBundle messages = ResourceBundle.getBundle("org.apache.aries.application.deployment.management.messages.DeploymentManagementMessages");
  private static class Message {
    public String msgKey;
    public Object[] inserts;
    public Exception cause;
    
    public Message(String msgKey, Exception cause, Object[] inserts) {
      this.msgKey = msgKey;
      this.cause = cause;
      this.inserts = inserts;
    }
  }
  
  private List<Message> errors = new ArrayList<Message>();
  private List<Message> warnings = new ArrayList<Message>();
  private final String fileName;
  
  public MessageUtil(String fileName) {
    this.fileName = fileName;
  }
  
  public String getFileName() {
    return fileName;
  }
  
  public void processMessages()
  {
    
      for (Message m : errors) {
        //Tr.error(tc, m.msgKey, m.inserts);
        // use logger
      }
    

    
      for (Message m : warnings) {
        //Tr.warning(tc, m.msgKey, m.inserts);
        // use logger
      }        
          
  }
  
  public List<String> getErrors()
  {
    List<String> result = new ArrayList<String>(warnings.size());
    for (Message m : warnings) {
      result.add(MessageFormat.format(messages.getString(m.msgKey), m.inserts));
    }
    
    return result;    
  }
  
  public List<String> getWarnings()
  {
    List<String> result = new ArrayList<String>(warnings.size());
    for (Message m : warnings) {
      result.add(MessageFormat.format(messages.getString(m.msgKey), m.inserts));
    }
    
    return result;
  }
  
  
  public void clear() 
  {
    errors.clear();
    warnings.clear();
  }
  
  public boolean hasErrors() 
  {
    return !!!errors.isEmpty();
  }
  
  public void error(String msgKey, Object ... inserts) 
  {
    errors.add(new Message(msgKey, null, inserts));
  }
  
  public void error(String msgKey, Exception e, Object ... inserts)
  {
    errors.add(new Message(msgKey, e, inserts));
  }
  
  public void warning(String msgKey, Object ... inserts)
  {
    warnings.add(new Message(msgKey, null, inserts));
  }
  
  public void warning(String msgKey, Exception e, Object ... inserts)
  {
    warnings.add(new Message(msgKey, e, inserts));
  }
  /**
   * Resolve a message from the bundle, including any necessary formatting.
   * 
   * @param key     the message key.
   * @param inserts any required message inserts.
   * @return        the message translated into the server local.
   */
  public static final String getMessage(String key, Object ... inserts)
  {
    String msg = messages.getString(key);
    
    if (inserts.length > 0)
      msg = MessageFormat.format(msg, inserts);
    
    return msg;
  }
}
