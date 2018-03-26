package org.apache.aries.osgi.functional;

import org.osgi.framework.ServiceReference;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class is an explicit wrapper around {@link ServiceReference} (it DOES NOT
 * implement {@link ServiceReference}) and provides methods to access underlying
 * {@link ServiceReference} properties and caches them so future access to the same
 * properties return the same values.
 *
 * Property values are cached <i>on demand</i>. Values that have never been
 * queried through the method are not cached.
 *
 * Properties that did not exist when queried will no longer exist even though
 * they were available at a later time in the underlying {@link ServiceReference}.
 *
 * Method {@link CachingServiceReference#isDirty()} will return t
 *
 * @author Carlos Sierra Andrés
 */
public class CachingServiceReference<T>
    implements Comparable<CachingServiceReference<T>> {

    public CachingServiceReference(ServiceReference<T> serviceReference) {
        _properties = new ConcurrentHashMap<>();
        _serviceReference = serviceReference;
    }

    @Override
    public int compareTo(CachingServiceReference<T> o) {
        Object myServiceRankingObject = getProperty("service.ranking");
        Object otherRankingObject = o.getProperty("service.ranking");

        if (myServiceRankingObject == null ||
            !(myServiceRankingObject instanceof Integer)) {
                myServiceRankingObject = 0;
        }
        if (otherRankingObject == null ||
            !(otherRankingObject instanceof Integer)) {
                otherRankingObject = 0;
        }
        int compare = Integer.compare(
            (Integer)myServiceRankingObject, (Integer)otherRankingObject);

        if (compare != 0) {
            return compare;
        }
        else {
            return Long.compare(
                (Long)o.getProperty("service.id"),
                (Long)_serviceReference.getProperty("service.id")
            );
        }
    }

    public String[] getCachedPropertyKeys() {
        return _properties.keySet().toArray(new String[0]);
    }

    /**
     * Returns the value associated with a key from the underlying {@link ServiceReference}
     * The returned value is then cached and the {@link ServiceReference} is
     * never queried again for the same value.
     *
     * Values that are not present in the {@link ServiceReference} return null.
     *
     * Values that were present in the moment they were first queried will return
     * the same value even if they disappear from they underlying {@link ServiceReference}.
     *
     * Values that were not present when queried the first time will continue to return
     * null even though they exist in future queries.
     * @param key the key of the property to be returned
     * @return the value associated with that key
     */
    public Object getProperty(String key) {
        Object propertyValue = _properties.compute(
            key,
            (__, value) -> {
                if (value == null) {
                    Object realValue = _serviceReference.getProperty(key);

                    if (realValue == null) {
                        return NULL.INSTANCE;
                    }

                    return realValue;
                } else {
                    return value;
                }
            }
        );

        if (propertyValue == NULL.INSTANCE) {
            return null;
        }

        return propertyValue;
    }

    /**
     * @return a union of the properties keys already cached by the instance
     * and the property keys available in the underlying {@link ServiceReference}.
     *
     * Cached property keys that returned null are not returned here.
     */
    public String[] getPropertyKeys() {
        Set<String> set = new HashSet<>();

        set.addAll(Arrays.asList(_serviceReference.getPropertyKeys()));
        set.addAll(_properties.keySet());

        List<String> nullProperties = _properties.entrySet().stream().filter(
            e -> e.getValue().equals(NULL.INSTANCE)
        ).map(
            Map.Entry::getKey
        ).collect(
            Collectors.toList()
        );

        set.removeAll(nullProperties);

        return set.toArray(new String[]{});
    }

    /**
     * @return The underlying {@link ServiceReference}
     */
    public ServiceReference<T> getServiceReference() {
        return _serviceReference;
    }

    @Override
    public int hashCode() {
        return _serviceReference.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CachingServiceReference<?> that = (CachingServiceReference<?>) o;

        return _serviceReference.equals(that._serviceReference);
    }
    /**
     * Checks if any of the cached properties has a different value in the
     * underlying {@link ServiceReference}. Only properties that have been
     * accessed through {@link CachingServiceReference#getProperty(String)} are
     * checked, so this method can't be used to know whether the underlying
     * {@link ServiceReference} han been altered since the creation of the
     * instance.
     *
     * @return true if any of the cached properties has a different value than
     * the ones held by the underlying {@link ServiceReference}
     */
    public boolean isDirty() {
        return _properties.entrySet().stream().anyMatch(
            e -> !e.getValue().equals(_serviceReference.getProperty(e.getKey()))
        );
    }

    @Override
    public String toString() {
        return "CachingServiceReference{" +
            "cachedProperties=" + _properties + ", " +
            "serviceReference=" + _serviceReference +
            '}';
    }

    /**
     * Checks if the property is dirty in this instance without caching the
     * value. Trying to do the same using getProperty would cache the property
     * which might not be desirable everytime.
     * @param key
     * @return
     */
    public boolean isDirty(String key) {
        Object value = _properties.get(key);

        return value != null &&
            !value.equals(_serviceReference.getProperty(key));
    }

    private final ConcurrentHashMap<String, Object> _properties;
    private final ServiceReference<T> _serviceReference;

    private static class NULL {
        private static NULL INSTANCE = new NULL();

        @Override
        public boolean equals(Object obj) {
            return ((this == obj) || (obj == null));
        }

        @Override
        public String toString() {
            return "null (cached)";
        }
    }

}
