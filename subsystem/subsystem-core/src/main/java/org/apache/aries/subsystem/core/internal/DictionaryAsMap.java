/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

public class DictionaryAsMap extends AbstractMap<String,String> {
    private Dictionary dict;

    public DictionaryAsMap(Dictionary dict) {
        this.dict = dict;
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return new AbstractSet<Entry<String, String>>() {
            @Override
            public Iterator<Entry<String, String>> iterator() {
                final Enumeration e = dict.keys();
                return new Iterator<Entry<String,String>>() {
                    public boolean hasNext() {
                        return e.hasMoreElements();
                    }

                    public Entry<String, String> next() {
                        Object key = e.nextElement();
                        Object val = dict.get(key);
                        return new SimpleImmutableEntry<String,String>(key != null ? key.toString() : null, val != null ? val.toString() : null);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
            @Override
            public int size() {
                return dict.size();
            }
        };
    }
}
