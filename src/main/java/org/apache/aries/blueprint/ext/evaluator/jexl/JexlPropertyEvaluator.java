package org.apache.aries.blueprint.ext.evaluator.jexl;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;

import org.apache.aries.blueprint.ext.PropertyEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JexlPropertyEvaluator implements PropertyEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JexlPropertyEvaluator.class);
    
    private JexlExpressionParser jexlParser;
    private Dictionary<String, String> properties;
    
    @Override
    public String evaluate(String expression, Dictionary<String, String> properties) {
        JexlExpressionParser parser = getJexlParser();
        this.properties = properties;

        Object obj;
        try {
            obj = parser.evaluate(expression);
            if (obj!=null) {
                return obj.toString();
            }
        } catch (Exception e) {
            LOGGER.info("Could not evaluate expression: {}", expression);
            LOGGER.info("Exception:", e);
        }
        
        return null;
    }
    
    private synchronized JexlExpressionParser getJexlParser() {
        if (jexlParser == null) {
            jexlParser = new JexlExpressionParser(toMap());
        }
        return jexlParser;
    }

    private Map<String, Object> toMap() {
        return new Map<String, Object>() {
            @Override
            public boolean containsKey(Object o) {
                return properties.get(o) != null;
            }
            
            @Override
            public Object get(Object o) {
                return properties.get(o);
            }
            
            // following are not important
            @Override
            public Object put(String s, Object o) {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public int size() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isEmpty() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsValue(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object remove(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(Map<? extends String, ? extends Object> map) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<String> keySet() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<Object> values() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<Entry<String, Object>> entrySet() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
