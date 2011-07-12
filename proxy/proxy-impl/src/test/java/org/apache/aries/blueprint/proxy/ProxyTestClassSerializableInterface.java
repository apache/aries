package org.apache.aries.blueprint.proxy;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class ProxyTestClassSerializableInterface implements
    ProxyTestSerializableInterface {

  public int value;
  
  /**
   * We deserialize using this static method to ensure that the right classloader
   * is used when deserializing our object, it will always be the classloader that
   * loaded this class, which might be the JUnit one, or our weaving one.
   * 
   * @param bytes
   * @param value
   * @throws Exception
   */
  public static void checkDeserialization(byte[] bytes, int value) throws Exception {
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
    ProxyTestClassSerializableInterface out = (ProxyTestClassSerializableInterface) ois.readObject();
    assertEquals(value, out.value);
  }
}
