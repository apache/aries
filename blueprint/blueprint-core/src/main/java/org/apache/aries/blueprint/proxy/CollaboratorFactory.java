package org.apache.aries.blueprint.proxy;

import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.proxy.InvocationListener;
import org.osgi.service.blueprint.reflect.ComponentMetadata;

import java.util.List;

public class CollaboratorFactory {
    public static InvocationListener create(ComponentMetadata componentMetadata, List<Interceptor> interceptors) {
        if (interceptors.size() == 1) {
            return new SingleInterceptorCollaborator(componentMetadata, interceptors.get(0));
        } else {
            return new Collaborator(componentMetadata, interceptors);
        }
    }
}
