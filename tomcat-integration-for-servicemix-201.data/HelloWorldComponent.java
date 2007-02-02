package org.servicemix.components;

import org.servicemix.components.util.TransformComponentSupport;
import org.servicemix.jbi.jaxp.StringSource;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

public class HelloWorldComponent extends TransformComponentSupport {

    private String property;
    
    protected boolean transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out)
            throws MessagingException {
        out.setContent(new StringSource("<hello>" + in.getProperty(property) + "</hello>"));
        return true;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

}
