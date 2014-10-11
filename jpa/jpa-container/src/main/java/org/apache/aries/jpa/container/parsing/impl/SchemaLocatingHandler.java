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
package org.apache.aries.jpa.container.parsing.impl;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This parser provides a quick mechanism for determining the JPA schema level,
 * and throws an EarlyParserReturn with the Schema to validate with
 */
public class SchemaLocatingHandler extends DefaultHandler
{
  /**
  * URI for the JPA persistence namespace 
  */
  private static final String PERSISTENCE_NS_URI = "http://java.sun.com/xml/ns/persistence";
  private static final String PERSISTENCE_21_NS_URI = "http://xmlns.jcp.org/xml/ns/persistence";
  
  /**
   * A static cache of schemas in use in the runtime
   */
  private static final ConcurrentMap<String, Schema> schemaCache = new ConcurrentHashMap<String, Schema>();
  
  @Override
  public void startElement(String uri, String localName, String name, Attributes attributes)
      throws SAXException
  {
    
    Schema s = null;
    String version = null;
    if((PERSISTENCE_NS_URI.equals(uri) || PERSISTENCE_21_NS_URI.equals(uri)) && "persistence".equals(localName) ) {
      version = attributes.getValue("version");
       s = validate(version);
    }
    throw new EarlyParserReturn(s, version);
  }
  
  /**
   * Find the schema for the version of JPA we're using
   * @param type  The value of the version attribute in the xml
   * @return
   * @throws SAXException
   */
  private final Schema validate(String type) throws SAXException
  {
    Schema toReturn = (type == null)? null : schemaCache.get(type);
    
    if(toReturn == null) {
      toReturn = getSchema(type);
      if(toReturn != null) schemaCache.putIfAbsent(type, toReturn);
    }
    
    return toReturn;
  }

  /**
   * Locate the schema document
   * @param type The schema version to find
   * @return
   * @throws SAXException
   */
  private final Schema getSchema(String type) throws SAXException
  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    URL schemaURL = null;
    if("1.0".equals(type)) {
      schemaURL = this.getClass().getResource("persistence.xsd.rsrc");
    } else if ("2.0".equals(type)) {
      schemaURL = this.getClass().getResource("persistence_2_0.xsd.rsrc");
    } else if ("2.1".equals(type)) {
      schemaURL = this.getClass().getResource("persistence_2_1.xsd.rsrc");
    }

    Schema schema = null;    
    if(schemaURL != null){
      schema = schemaFactory.newSchema(schemaURL);
    }
    
    return schema;
  }
  
}
