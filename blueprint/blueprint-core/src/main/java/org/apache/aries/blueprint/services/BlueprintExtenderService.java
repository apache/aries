package org.apache.aries.blueprint.services;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.BlueprintContainer;

public interface BlueprintExtenderService {

    /**
     * Create Blueprint container for the application bundle 
     * @param bundle the application bundle
     * @return container
     */
    BlueprintContainer createContainer(Bundle bundle);

    /**
     * Create Blueprint container for the application bundle using a list of Blueprint resources 
     * @param bundle the application bundle
     * @param blueprintPaths the application blueprint resources
     * @return container
     */    
    BlueprintContainer createContainer(Bundle bundle, List<Object> blueprintPaths);

    /**
     * Get an existing container for the application bundle
     * @param bundle the application bundle
     * @return container
     */
    BlueprintContainer getContainer(Bundle bundle);

    /**
     * Destroy Blueprint container for the application bundle
     * @param bundle the application bundle
     * @param container the container
     */
    void destroyContainer(Bundle bundle, BlueprintContainer container);
}

