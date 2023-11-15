/*
 * Copyright (c) OSGi Alliance (2008, 2009). All Rights Reserved.
 *
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

package org.apache.aries.blueprint.parser;

import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A ComponentDefinitionException with specifying a node in the blueprint XML.
 * It will expand the explanation with the node but does not store the node.
 *
 * @version $Revision$
 */
public class ComponentDefinitionElementException extends ComponentDefinitionException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a Component Definition Exception with no message or exception
     * cause.
     */
    public ComponentDefinitionElementException() {
        super();
    }

    /**
     * Creates a Component Definition Exception with the specified message.
     * The node is regarded as the cause path and will be added to the explanation.
     *
     * @param node        The element path in which the exception originates.
     * @param explanation The associated message.
     */
    public ComponentDefinitionElementException(Node node, String explanation) {
        super(getElementPathForError(node) + ": " + explanation);
    }

    /**
     * Creates a Component Definition Exception with the specified message and
     * exception cause.
     * The node is regarded as the cause path and will be added to the explanation.
     *
     * @param node        The element path in which the exception originates.
     * @param explanation The associated message.
     * @param cause       The cause of this exception.
     */
    public ComponentDefinitionElementException(Node node, String explanation, Throwable cause) {
        super(getElementPathForError(node) + ": " + explanation, cause);
    }

    private static String getElementPathForError(Node node) {
        if (node == null) {
            return "<none>";
        }
        StringBuilder result = new StringBuilder();
        while (node != null) {
            if (node instanceof Element) {
                Element element = (Element) node;

                String errorElement = "<"
                        + element.getLocalName()
                        // the following are possibly helping attributes to find the malfunction path
                        + getErrorPathAttributeString(element, Parser.ID_ATTRIBUTE)
                        + getErrorPathAttributeString(element, Parser.NAME_ATTRIBUTE)
                        + getErrorPathAttributeString(element, Parser.KEY_ATTRIBUTE)
                        + getErrorPathAttributeString(element, Parser.CLASS_ATTRIBUTE)
                        + ">";
                result.insert(0, errorElement); // prefix the parent
            }
            node = node.getParentNode();
        }
        return result.toString();
    }

    private static String getErrorPathAttributeString(Element element, String attributeName) {
        if (element.hasAttribute(attributeName)) {
            return " " + attributeName
                    + "=\"" + element.getAttribute(attributeName) + "\"";
        }
        return "";
    }

}
