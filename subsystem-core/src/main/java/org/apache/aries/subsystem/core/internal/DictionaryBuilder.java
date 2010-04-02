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

import java.util.Dictionary;
import java.util.Hashtable;

public class DictionaryBuilder<K,V> {

    public static <K,V> Dictionary<K,V> build(K k, V v) {
        return new DictionaryBuilder<K,V>().p(k, v).get();
    }

    public static <K,V> Dictionary<K,V> build(K k1, V v1, K k2, V v2) {
        return new DictionaryBuilder<K,V>().p(k1, v1).p(k2, v2).get();
    }

    public static <K,V> Dictionary<K,V> build(K k1, V v1, K k2, V v2, K k3, V v3) {
        return new DictionaryBuilder<K,V>().p(k1, v1).p(k2, v2).p(k3, v3).get();
    }

    private Dictionary<K,V> dict;

    public DictionaryBuilder() {
        dict = new Hashtable<K,V>();
    }

    DictionaryBuilder<K,V> p(K k, V v) {
        dict.put(k, v);
        return this;
    }

    Dictionary<K,V> get() {
        return dict;
    }
}
