package org.apache.aries.jndi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;

import org.apache.aries.jndi.startup.Activator;
import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.jndi.JNDIConstants;

public class InitialContextTest 
{
  private Activator activator;
  private BundleContext bc;
  
  /**
   * This method does the setup .
   * @throws NoSuchFieldException 
   * @throws SecurityException 
   * @throws IllegalAccessException 
   * @throws IllegalArgumentException 
   */
  @Before
  public void setup() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
  {
    bc =  Skeleton.newMock(new BundleContextMock(), BundleContext.class);
    activator = new Activator();
    activator.start(bc);
  }

  /**
   * Make sure we clear the caches out before the next test.
   */
  @After
  public void teardown()
  {
    activator.stop(bc);
    BundleContextMock.clear();
  }

  @Test
  public void testLookupWithICF() throws NamingException
  {
    InitialContextFactory icf = Skeleton.newMock(InitialContextFactory.class);
    bc.registerService(new String[] {InitialContextFactory.class.getName(), icf.getClass().getName()}, icf, new Properties());
    Skeleton.getSkeleton(icf).setReturnValue(new MethodCall(Context.class, "lookup", "/"), Skeleton.newMock(Context.class));
    
    Properties props = new Properties();
    props.put(Context.INITIAL_CONTEXT_FACTORY, icf.getClass().getName());
    props.put(JNDIConstants.BUNDLE_CONTEXT, bc);
    InitialContext ctx = new InitialContext(props);
    
    Context namingCtx = (Context) ctx.lookup("/");
    assertTrue("Context returned isn't the raw naming context: " + namingCtx, Skeleton.isSkeleton(namingCtx));
  }
  
  @Test(expected=NoInitialContextException.class)
  public void testLookupWithoutICF() throws NamingException
  {
    Properties props = new Properties();
    props.put(JNDIConstants.BUNDLE_CONTEXT, bc);
    InitialContext ctx = new InitialContext(props);
    
    ctx.lookup("/");
  }

  @Test
  public void testLookupWithoutICFButWithURLLookup() throws NamingException
  {
    ObjectFactory factory = Skeleton.newMock(ObjectFactory.class);
    Context ctx = Skeleton.newMock(Context.class);
    Skeleton.getSkeleton(factory).setReturnValue(new MethodCall(ObjectFactory.class, "getObjectInstance", Object.class, Name.class, Context.class, Hashtable.class),
                                                 ctx);
    Skeleton.getSkeleton(ctx).setReturnValue(new MethodCall(Context.class, "lookup", String.class), "someText");
    
    Properties props = new Properties();
    props.put(JNDIConstants.JNDI_URLSCHEME, "testURL");
    bc.registerService(ObjectFactory.class.getName(), factory, props);
    
    
    props = new Properties();
    props.put(JNDIConstants.BUNDLE_CONTEXT, bc);
    InitialContext initialCtx = new InitialContext(props);
    
    Object someObject = initialCtx.lookup("testURL:somedata");
    assertEquals("Expected to be given a string, but got something else.", "someText", someObject);
  }
}