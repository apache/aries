package org.apache.aries.blueprint.ext;

import java.util.Dictionary;


public interface PropertyEvaluator {
    public String evaluate(String expression, Dictionary<String, String> properties);
}
