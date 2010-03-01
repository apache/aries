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

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;

import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.apache.aries.jpa.container.parsing.PersistenceDescriptor;
import org.apache.aries.jpa.container.parsing.PersistenceDescriptorParser;
import org.apache.aries.jpa.container.parsing.PersistenceDescriptorParserException;
import org.osgi.framework.Bundle;

/**
 * This class may be used to parse JPA persistence descriptors. The parser validates
 * using the relevant version of the persistence schema as defined by the xml file. 
 */
public class PersistenceDescriptorParserImpl implements PersistenceDescriptorParser {

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
  
  /* (non-Javadoc)
   * @see org.apache.aries.jpa.container.parsing.impl.PersistenceDescriptorParser#parse(org.osgi.framework.Bundle, org.apache.aries.jpa.container.parsing.PersistenceDescriptor)
   */
  public Collection<ParsedPersistenceUnit> parse(Bundle b, PersistenceDescriptor descriptor) throws PersistenceDescriptorParserException {
    Collection<ParsedPersistenceUnit> persistenceUnits = new ArrayList<ParsedPersistenceUnit>();
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    BufferedInputStream is = null;
    boolean schemaFound = false;
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
          schemaFound = true;
          parserFactory.setSchema(s);
          parserFactory.setNamespaceAware(true);
          parser = parserFactory.newSAXParser();
         
          //Get back to the beginning of the stream
          is.reset();
          
          JPAHandler handler = new JPAHandler(b, epr.getVersion());
          parser.parse(is, handler);
          persistenceUnits.addAll(handler.getPersistenceUnits());
        } 
      }
    } catch (Exception e) {
      throw new PersistenceDescriptorParserException("There was an error parsing " + descriptor.getLocation() 
          + " in bundle " + b.getSymbolicName() + "_" + b.getVersion(), e);
    } finally {
      if(is != null) try {
        is.close();
      } catch (IOException e) {
        //No logging necessary, just consume
      }
    }
    if(!!!schemaFound) {
    throw new PersistenceDescriptorParserException("No Schema could be located for the" +
        "persistence descriptor " + descriptor.getLocation() 
        + " in bundle " + b.getSymbolicName() + "_" + b.getVersion());
    }
    return persistenceUnits;
  }

}
