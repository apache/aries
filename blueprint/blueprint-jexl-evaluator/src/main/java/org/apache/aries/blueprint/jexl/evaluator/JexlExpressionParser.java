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


package org.apache.aries.blueprint.jexl.evaluator;

import java.util.Map;

import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev: 907189 $ $Date: 2010-02-06 16:01:43 +0800 (Sat, 06 Feb 2010) $
 */
public class JexlExpressionParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(JexlExpressionParser.class);

    protected final JexlContext context;
    private final JexlEngine engine;
    
    public JexlExpressionParser(final Map<String, Object> vars) {
        if (vars == null) {
            throw new IllegalArgumentException("vars: " + vars);
        }
        engine = new JexlEngine();
        context = new MapContext(vars);

        LOGGER.trace("Using variables: {}", vars);
    }

    public Object evaluate(final String expression) throws Exception {
        if (expression == null) {
            throw new IllegalArgumentException("expression: " + expression);
        }

        LOGGER.trace("Evaluating expression: {}", expression);
        return engine.createExpression(expression).evaluate(context);

    }

}
