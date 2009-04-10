package org.apache.felix.blueprint.pojos;

import java.net.URI;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Mar 26, 2009
 * Time: 6:16:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class PojoB {

    private URI uri;

    public PojoB() {
    }

    public PojoB(URI uri) {
        this.uri = uri;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}
