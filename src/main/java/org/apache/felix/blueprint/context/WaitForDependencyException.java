package org.apache.felix.blueprint.context;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Apr 14, 2009
 * Time: 12:17:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class WaitForDependencyException extends Exception {

    public WaitForDependencyException() {
    }

    public WaitForDependencyException(String message) {
        super(message);
    }
}
