/*
 * IBM Confidential
 * 
 * OCO Source Materials
 * 
 * Copyright IBM Corp. 2009
 * 
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 * 
 * Change activity:
 * 
 * Issue       Date        Name     Description
 * ----------- ----------- -------- ------------------------------------
 */
package org.apache.aries.application.utils;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.ApplicationMetadataManager;
import org.apache.aries.application.impl.ApplicationMetadataManagerImpl;
import org.junit.Test;

public class ApplicationMetadataImplTest
{
  @Test
  public void testBasicMetadataCreation() throws IOException
  {
    ApplicationMetadataManager manager = new ApplicationMetadataManagerImpl();
    ApplicationMetadata app = manager.parseApplication(getClass().getResourceAsStream("/META-INF/APPLICATION.MF"));
    
    Assert.assertEquals("Travel Reservation", app.getApplicationName());
  }
}