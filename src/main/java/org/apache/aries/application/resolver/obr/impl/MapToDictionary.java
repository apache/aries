/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.aries.application.resolver.obr.impl;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

/**
 * @version $Rev$ $Date$
 */
public class MapToDictionary extends Dictionary
{
  /**
   * Map source.
   */
  private final Map m_map;

  public MapToDictionary(Map map)
  {
    m_map = map;
  }

  public Enumeration elements()
  {
    if (m_map == null) {
      return null;
    }
    return new IteratorToEnumeration(m_map.values().iterator());
  }

  public Object get(Object key)
  {
    if (m_map == null) {
      return null;
    }
    return m_map.get(key);
  }

  public boolean isEmpty()
  {
    if (m_map == null) {
      return true;
    }
    return m_map.isEmpty();
  }

  public Enumeration keys()
  {
    if (m_map == null) {
      return null;
    }
    return new IteratorToEnumeration(m_map.keySet().iterator());
  }

  public Object put(Object key, Object value)
  {
    throw new UnsupportedOperationException();
  }

  public Object remove(Object key)
  {
    throw new UnsupportedOperationException();
  }

  public int size()
  {
    if (m_map == null) {
      return 0;
    }
    return m_map.size();
  }

  private static class IteratorToEnumeration implements Enumeration
  {
    private final Iterator m_iter;

    public IteratorToEnumeration(Iterator iter)
    {
      m_iter = iter;
    }

    public boolean hasMoreElements()
    {
      if (m_iter == null)
        return false;
      return m_iter.hasNext();
    }

    public Object nextElement()
    {
      if (m_iter == null)
        return null;
      return m_iter.next();
    }
  }

}
