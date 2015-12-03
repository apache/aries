package org.apache.aries.subsystem.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;

public class InstallDependencies {
    public void install(BasicSubsystem subsystem, BasicSubsystem parent, Coordination coordination) throws Exception{
        // Install dependencies first...
        List<Resource> dependencies = new ArrayList<Resource>(subsystem.getResource().getInstallableDependencies());
        Collections.sort(dependencies, new InstallResourceComparator());
        for (Resource dependency : dependencies)
            ResourceInstaller.newInstance(coordination, dependency, subsystem).install();
        for (Resource dependency : subsystem.getResource().getSharedDependencies()) {
            // TODO This needs some more thought. The following check
            // protects against a child subsystem that has its parent as a
            // dependency. Are there other places of concern as well? Is it
            // only the current parent that is of concern or should all
            // parents be checked?
            if (parent==null || !dependency.equals(parent))
                ResourceInstaller.newInstance(coordination, dependency, subsystem).install();
        }
    }
}
