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
package org.apache.aries.blueprint.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.aries.blueprint.proxy.ProxyTestClassInnerClasses.ProxyTestClassInner;
import org.apache.aries.blueprint.proxy.ProxyTestClassInnerClasses.ProxyTestClassStaticInner;
import org.apache.aries.blueprint.proxy.ProxyTestClassInnerClasses.ProxyTestClassUnweavableInnerChild;
import org.apache.aries.blueprint.proxy.pkg.ProxyTestClassUnweavableSuperWithDefaultMethodWrongPackageParent;
import org.apache.aries.proxy.FinalModifierException;
import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.proxy.impl.AsmProxyManager;
import org.apache.aries.proxy.impl.SingleInstanceDispatcher;
import org.apache.aries.proxy.impl.SystemModuleClassLoader;
import org.apache.aries.proxy.impl.gen.ProxySubclassMethodHashSet;
import org.apache.aries.proxy.impl.weaving.WovenProxyGenerator;
import org.apache.aries.proxy.weaving.WovenProxy;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.apache.aries.util.ClassLoaderProxy;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;


public class WovenProxyGeneratorTest extends AbstractProxyTest
{
  private static final String hexPattern = "[0-9_a-f]";
  private static final int[] uuid_pattern = new int[] {8,4,4,4,12};
  private static final String regexp;
  
  static {
    StringBuilder sb = new StringBuilder(".*");
    for(int i : uuid_pattern) {
      for(int j = 0; j < i; j++){
        sb.append(hexPattern);
      }
      sb.append("_");
    }
    sb.deleteCharAt(sb.length() -1);
    sb.append("\\d*");
    regexp = sb.toString();
  }
  
  /** An array of classes that will be woven - note no UnweavableParents should be in here! */
  private static final List<Class<?>> CLASSES = Arrays.asList(new Class<?>[]{ProxyTestClassGeneral.class, ProxyTestClassSuper.class,
    ProxyTestClassFinalMethod.class, ProxyTestClassFinal.class, ProxyTestClassGeneric.class,
    ProxyTestClassGenericSuper.class, ProxyTestClassCovariant.class, ProxyTestClassCovariantOverride.class,
    ProxyTestClassUnweavableChild.class, ProxyTestClassUnweavableSibling.class, ProxyTestClassInner.class, 
    ProxyTestClassStaticInner.class, ProxyTestClassUnweavableInnerChild.class, 
    ProxyTestClassUnweavableChildWithFinalMethodParent.class, 
    ProxyTestClassUnweavableChildWithDefaultMethodWrongPackageParent.class, 
    ProxyTestClassSerializable.class, ProxyTestClassSerializableWithSVUID.class,
    ProxyTestClassSerializableChild.class, ProxyTestClassSerializableInterface.class,
    ProxyTestClassStaticInitOfChild.class, ProxyTestClassAbstract.class});
  
  /** An array of classes that are loaded by the WeavingLoader, but not actually woven **/
  private static final List<Class<?>> OTHER_CLASSES = Arrays.asList(new Class<?>[] {ProxyTestClassUnweavableSuper.class,
		  ProxyTestClassStaticInitOfChildParent.class, ProxyTestClassChildOfAbstract.class});
 
  private static final Map<String, byte[]> rawClasses = new HashMap<String, byte[]>();
  
  protected static final ClassLoader weavingLoader = new SystemModuleClassLoader() {
    public Class<?> loadClass(String className)  throws ClassNotFoundException
    {
      return loadClass(className, false);
    }
    public Class<?> loadClass(String className, boolean b) throws ClassNotFoundException
    {
      if (!!!className.startsWith("org.apache.aries.blueprint.proxy.ProxyTest")){
        return Class.forName(className);
      }
      
      Class<?> clazz = findLoadedClass(className);
      if(clazz != null)
        return clazz;
      
      byte[] bytes = rawClasses.get(className);
      if(bytes == null)
        return super.loadClass(className, b);
      
      boolean weave = false;
      
      for(Class<?> c : CLASSES) {
        if(c.getName().equals(className)) {
          weave = true;
          break;
        }
      }
      if(weave)
        bytes = WovenProxyGenerator.getWovenProxy(bytes, this);
      
      return defineClass(className, bytes, 0, bytes.length);
    }
    
    protected URL findResource(String resName) {
      return WovenProxyGeneratorTest.class.getResource(resName);
    }
  };
   
  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUp() throws Exception
  {
    List<Class<?>> classes = new ArrayList(CLASSES.size() + OTHER_CLASSES.size());
    
    classes.addAll(CLASSES);
    classes.addAll(OTHER_CLASSES);
    
    for(Class<?> clazz : classes) {
      InputStream is = clazz.getClassLoader().getResourceAsStream(
          clazz.getName().replace('.', '/') + ".class");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      
      byte[] buffer = new byte[2048];
      int read = is.read(buffer);
      while(read != -1) {
        baos.write(buffer, 0, read);
        read = is.read(buffer);
      }
      rawClasses.put(clazz.getName(), baos.toByteArray());
    }
  }

  /**
   * This test uses the WovenProxyGenerator to generate and load the specified getTestClass().
   * 
   * Once the subclass is generated we check that it wasn't null. 
   * 
   * Test method for
   * {@link WovenProxyGenerator#getProxySubclass(byte[], String)}.
   */
  @Test
  public void testGenerateAndLoadProxy() throws Exception
  {
    super.testGenerateAndLoadProxy();
    assertTrue("Should be a WovenProxy", WovenProxy.class.isAssignableFrom(getProxyClass(getTestClass())));
  }

  /**
   * Test that the methods found declared on the generated proxy are
   * the ones that we expect.
   */
  @Test
  public void testExpectedMethods() throws Exception
  {
    ProxySubclassMethodHashSet<String> originalMethods = getMethods(getTestClass());

    ProxySubclassMethodHashSet<String> generatedMethods = getMethods(weavingLoader.
        loadClass(getTestClass().getName()));

    // check that all the methods we have generated were expected
    for (String gen : generatedMethods) {
      assertTrue("Unexpected method: " + gen, originalMethods.contains(gen));
    }
    // check that all the expected methods were generated
    for (String exp : originalMethods) {
      assertTrue("Method was not generated: " + exp, generatedMethods.contains(exp));
    }
    // check the sets were the same
    assertEquals("Sets were not the same", originalMethods, generatedMethods);
  }

  private ProxySubclassMethodHashSet<String> getMethods(Class<?> clazz) {
    
    ProxySubclassMethodHashSet<String> foundMethods = 
      new ProxySubclassMethodHashSet<String>(12);
    do {
      Method[] declaredMethods = clazz.getDeclaredMethods();
      List<Method> listOfDeclaredMethods = new ArrayList<Method>();
      for (Method m : declaredMethods) {
        if(m.getName().startsWith(WovenProxy.class.getName().replace('.', '_')) ||
            m.getName().startsWith("getListener") || m.getName().startsWith("getInvocationTarget") ||
            //four hex digits
            m.getName().matches(regexp))
          continue;
        
        listOfDeclaredMethods.add(m);
      }
      declaredMethods = listOfDeclaredMethods.toArray(new Method[] {});
      foundMethods.addMethodArray(declaredMethods);
      clazz = clazz.getSuperclass();
    } while (clazz != null);
    return foundMethods;
  }

  /**
   * Test a method marked final
   */
  @Test
  public void testFinalMethod() throws Exception
  {
    assertNotNull(weavingLoader.loadClass(ProxyTestClassFinalMethod.class
        .getName()));
  }

  /**
   * Test a class marked final
   */
  @Test
  public void testFinalClass() throws Exception
  {
    assertNotNull(weavingLoader.loadClass(ProxyTestClassFinal.class
        .getName()));
  }

  /**
   * Test a private constructor
   */
  @Test
  public void testPrivateConstructor() throws Exception
  {
    assertNotNull(weavingLoader.loadClass(ProxyTestClassFinal.class
        .getName()));
  }
  
  /**
   * Test a class whose super couldn't be woven
   */
  @Test
  public void testUnweavableSuper() throws Exception
  {
    Class<?> woven = getProxyClass(ProxyTestClassUnweavableChild.class);
    
    assertNotNull(woven);
    assertNotNull(getProxyInstance(woven));
    
    TestListener tl = new TestListener();
    Object ptcuc = getProxyInstance(woven, tl);
    assertCalled(tl, false, false, false);
    
    Method m = ptcuc.getClass().getMethod("doStuff");
    
    assertEquals("Hi!", m.invoke(ptcuc));
    
    assertCalled(tl, true, true, false);
    
    assertEquals(ProxyTestClassUnweavableGrandParent.class.getMethod("doStuff"), 
        tl.getLastMethod());
    
    tl.clear();

    //Because default access works on the package, and we are defined on a different classloader
    //we have to call setAccessible...
    
    m = getDeclaredMethod(ProxyTestClassUnweavableChild.class, "doStuff2");
    m.setAccessible(true);
    assertEquals("Hello!", m.invoke(ptcuc));
    
    assertCalled(tl, true, true, false);
    
    assertEquals(weavingLoader.loadClass(ProxyTestClassUnweavableSuper.class.getName()).getDeclaredMethod("doStuff2"),
    		tl.getLastMethod());
  }
  
  @Test
  public void testUnweavableSuperWithNoNoargsAllTheWay() throws Exception
  {
    try {
      getProxyClass(ProxyTestClassUnweavableSibling.class);
      fail();
    } catch (RuntimeException re) {
      assertTrue(re.getCause() instanceof UnableToProxyException);
      assertEquals(ProxyTestClassUnweavableSibling.class.getName(),
          ((UnableToProxyException)re.getCause()).getClassName());
    }
  }  
  
  /**
   * Test a class whose super couldn't be woven
   */
  @Test
  public void testUnweavableSuperWithFinalMethod() throws Exception
  {
    try{
      getProxyClass(ProxyTestClassUnweavableChildWithFinalMethodParent.class);
      fail();
    } catch (RuntimeException re) {
      assertTrue(re.getCause() instanceof FinalModifierException);
      assertEquals(ProxyTestClassUnweavableSuperWithFinalMethod.class.getName(),
          ((FinalModifierException)re.getCause()).getClassName());
      assertEquals("doStuff2", ((FinalModifierException)re.getCause())
          .getFinalMethods());
    }
  }
  
  /**
   * Test a class whose super couldn't be woven
   */
  @Test
  public void testUnweavableSuperWithDefaultMethodInWrongPackage() throws Exception
  {
    try{
      getProxyClass(ProxyTestClassUnweavableChildWithDefaultMethodWrongPackageParent.class);
      fail();
    } catch (RuntimeException re) {
      assertTrue(re.getCause() instanceof UnableToProxyException);
      assertEquals(ProxyTestClassUnweavableSuperWithDefaultMethodWrongPackageParent
          .class.getName(), ((UnableToProxyException)re.getCause()).getClassName());
    }
  }
  
  @Test
  public void testInnerWithNoParentNoArgs() throws Exception {
    //An inner class has no no-args (the parent gets added as an arg) so we can't
    //get an instance
    try{
      getProxyClass(ProxyTestClassUnweavableInnerChild.class);
      fail();
    } catch (RuntimeException re) {
      assertTrue(re.getCause() instanceof UnableToProxyException);
      assertEquals(ProxyTestClassUnweavableInnerChild.class.getName(), 
          ((UnableToProxyException)re.getCause()).getClassName());
    }
  }
  
  @Test(expected=NoSuchFieldException.class)
  public void testNonSerializableClassHasNoGeneratedSerialVersionUID() throws Exception {
    Class<?> woven = getProxyClass(getTestClass());
    woven.getDeclaredField("serialVersionUID");
  }
  
  @Test
  public void testSerialization() throws Exception {
    
    ProxyTestClassSerializable in = new ProxyTestClassSerializable();
    in.value = 5;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(in);
    
    ProxyTestClassSerializable.checkDeserialization(baos.toByteArray(), 5);

    Class<?> woven = getProxyClass(ProxyTestClassSerializable.class);
    
    woven.getMethod("checkDeserialization", byte[].class, int.class).invoke(null, baos.toByteArray(), 5);
  }
  
  @Test
  public void testInheritedSerialization() throws Exception {
    
    ProxyTestClassSerializableChild in = new ProxyTestClassSerializableChild();
    in.value = 4;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(in);
    
    ProxyTestClassSerializable.checkDeserialization(baos.toByteArray(), 4);

    Class<?> woven = getProxyClass(ProxyTestClassSerializable.class);
    
    woven.getMethod("checkDeserialization", byte[].class, int.class).invoke(null, baos.toByteArray(), 4);
  }
  
  @Test
  public void testInterfaceInheritedSerialization() throws Exception {
    
    ProxyTestClassSerializableInterface in = new ProxyTestClassSerializableInterface();
    in.value = 3;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(in);
    
    ProxyTestClassSerializableInterface.checkDeserialization(baos.toByteArray(), 3);

    Class<?> woven = getProxyClass(ProxyTestClassSerializableInterface.class);
    
    woven.getMethod("checkDeserialization", byte[].class, int.class).invoke(null, baos.toByteArray(), 3);
  }
  
  @Test
  public void testGeneratedSVUIDisSynthetic() throws Exception {
    
    Class<?> woven = getProxyClass(ProxyTestClassSerializable.class);
    
    assertTrue(woven.getDeclaredField("serialVersionUID").isSynthetic());
    
    woven = getProxyClass(ProxyTestClassSerializableWithSVUID.class);
    
    assertFalse(woven.getDeclaredField("serialVersionUID").isSynthetic());
  }
  
  /**
   * This test covers a weird case on Mac VMs where we sometimes
   * get a ClassCircularityError if a static initializer in a
   * non-woven superclass references a subclass that's being
   * woven, and gets triggered by the weaving process. Not known
   * to fail on IBM or Sun/Oracle VMs
   */
  @Test
  public void testSuperStaticInitOfChild() throws Exception {
    Class<?> parent = weavingLoader.loadClass(ProxyTestClassStaticInitOfChildParent.class.getName());
    parent.getMethod("doStuff").invoke(null);
  }

  @Override
  protected Object getProxyInstance(Class<?> proxyClass) {
    try {
      if(proxyClass.getName().equals(ProxyTestClassAbstract.class.getName())) {
        Collection<Class<?>> coll = new ArrayList<Class<?>>();
        coll.add(proxyClass);
        return new AsmProxyManager().createNewProxy(null, coll, new Callable() {
          public Object call() throws Exception {
            return null;
          }}, null);
      }
      return proxyClass.newInstance();
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  protected Class<?> getProxyClass(Class<?> clazz) {
    try {
      return weavingLoader.loadClass(clazz.getName());
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  @Override
  protected Object setDelegate(Object proxy, Callable<Object> dispatcher) {
    return ((WovenProxy) proxy).
    org_apache_aries_proxy_weaving_WovenProxy_createNewProxyInstance(
        dispatcher, null);
  }

  @Override
  protected Object getProxyInstance(Class<?> proxyClass,
      InvocationListener listener) {
    WovenProxy proxy = (WovenProxy) getProxyInstance(proxyClass);
    proxy = proxy.org_apache_aries_proxy_weaving_WovenProxy_createNewProxyInstance(
        new SingleInstanceDispatcher(proxy), listener);
    return proxy;
  }
  
  protected Object getP3() {
    return getProxyInstance(getProxyClass(getTestClass()));
  }
  
  /**
   * This tests that the Synthesizer ran correctly and the packaged
   * WovenProxy class has been modified with the synthetic modifier
   */
  @Test
  public void testWovenProxyIsSynthetic(){
    assertTrue(WovenProxy.class.isSynthetic());
  }
  
  /**
   * This test checks that we can add interfaces to classes that don't implement
   * them using dynamic subclassing. This is a little odd, but it came for
   * free with support for proxying abstract classes!
   * @throws Exception 
   */
  @Test
  public void testWovenClassPlusInterfaces() throws Exception {
    Bundle b = (Bundle) Skeleton.newMock(new Class<?>[] {Bundle.class, ClassLoaderProxy.class});
    BundleWiring bw = (BundleWiring) Skeleton.newMock(BundleWiring.class);

    Skeleton.getSkeleton(b).setReturnValue(new MethodCall(
        ClassLoaderProxy.class, "getClassLoader"), weavingLoader);
    Skeleton.getSkeleton(b).setReturnValue(new MethodCall(
        ClassLoaderProxy.class, "adapt", BundleWiring.class), bw);

    Object toCall = new AsmProxyManager().createDelegatingProxy(b, Arrays.asList(
        getProxyClass(ProxyTestClassAbstract.class), Callable.class), new Callable() {

          public Object call() throws Exception {
            return weavingLoader.loadClass(ProxyTestClassChildOfAbstract.class.getName()).newInstance();
          }
      
    }, null);
    
    //Should proxy the abstract method on the class
    Method m = getProxyClass(ProxyTestClassAbstract.class).getMethod("getMessage");
    assertEquals("Working", m.invoke(toCall));
    
    //Should be a callable too!
    assertEquals("Callable Works too!", ((Callable)toCall).call());
    
  }
}

