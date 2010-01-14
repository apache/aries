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

package org.apache.aries.jpa.container.parsing;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;

import org.apache.aries.jpa.container.parsing.impl.EarlyParserReturn;
import org.apache.aries.jpa.container.parsing.impl.JPAHandler;
import org.apache.aries.jpa.container.parsing.impl.SchemaLocatingHandler;
import org.osgi.framework.Bundle;

/**
 * This class may be used to parse JPA persistence descriptors. The parser validates
 * using the relevant version of the persistence schema as defined by the xml file. 
 */
public class PersistenceDescriptorParser {

  /**
   * This class is used internally to prevent the first pass parse from
   * closing the InputStream when it exits.
   */
  private static class UnclosableInputStream extends FilterInputStream {

    public UnclosableInputStream(InputStream in) {
      super(in);
    }

    @Override
    public void close() throws IOException {
      //No op, don't close the parent.
    }
  }
  
  /**
   * Parse the supplied {@link PersistenceDescriptor} 
   * 
   * @param b  The bundle that contains the persistence descriptor
   * @param descriptor The descriptor
   * 
   * @return A collection of {@link ParsedPersistenceUnit}
   * @throws PersistenceDescriptorParserException  if any error occurs in parsing
   */
  public static Collection<ParsedPersistenceUnit> parse(Bundle b, PersistenceDescriptor descriptor) throws PersistenceDescriptorParserException {
    Collection<ParsedPersistenceUnit> persistenceUnits = new ArrayList<ParsedPersistenceUnit>();
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    BufferedInputStream is = null;
    try {
      //Buffer the InputStream so we can mark it, though we'll be in 
      //trouble if we have to read more than 8192 characters before finding
      //the schema!
      is = new BufferedInputStream(descriptor.getInputStream(), 8192);
      is.mark(8192);
      SAXParser parser = parserFactory.newSAXParser();
      try{
        parser.parse(new UnclosableInputStream(is), new SchemaLocatingHandler());
      } catch (EarlyParserReturn epr) {
        //This is not really an exception, but a way to work out which
        //version of the persistence schema to use in validation
        Schema s = epr.getSchema();
        
        if(s != null) {
          parserFactory.setSchema(s);
          parserFactory.setNamespaceAware(true);
          parser = parserFactory.newSAXParser();
         
          //Get back to the beginning of the stream
          is.reset();
          
          JPAHandler handler = new JPAHandler(b, epr.getVersion());
          parser.parse(is, handler);
          persistenceUnits.addAll(handler.getPersistenceUnits());
        } else {
          //TODO Should we try without validation?
        }
      }
    } catch (Exception e) {
      //TODO Log this error in parsing
      System.out.println("Error parsing " + descriptor.getLocation() + " in bundle " + b.getSymbolicName() + "_" + b.getVersion());
      e.printStackTrace();
      throw new PersistenceDescriptorParserException(e);
    } finally {
      if(is != null) try {
        is.close();
      } catch (IOException e) {
        //TODO Log this
        e.printStackTrace();
      }
    }
    return persistenceUnits;
  }

}
