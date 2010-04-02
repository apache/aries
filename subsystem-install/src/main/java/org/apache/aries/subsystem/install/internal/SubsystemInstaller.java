/*
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
package org.apache.aries.subsystem.install.internal;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.SubsystemAdmin;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.felix.fileinstall.ArtifactInstaller;

public class SubsystemInstaller implements ArtifactInstaller {

    private SubsystemAdmin subsystemAdmin;

    public SubsystemAdmin getSubsystemAdmin() {
        return subsystemAdmin;
    }

    public void setSubsystemAdmin(SubsystemAdmin subsystemAdmin) {
        this.subsystemAdmin = subsystemAdmin;
    }

    public void install(File file) throws Exception {
        if (file.isDirectory()) {
            subsystemAdmin.install("jardir:" + file.getPath());
        } else {
            subsystemAdmin.install(file.toURI().toURL().toExternalForm());
        }
    }

    public void update(File file) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void uninstall(File file) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean canHandle(File artifact)
    {
        JarFile jar = null;
        try
        {
            // Handle OSGi bundles with the default deployer
            String name = artifact.getName();
            if (!artifact.canRead()
                || name.endsWith(".txt") || name.endsWith(".xml")
                || name.endsWith(".properties") || name.endsWith(".cfg"))
            {
                // that's file type which is not supported as bundle and avoid
                // exception in the log
                return false;
            }
            jar = new JarFile(artifact);
            Manifest m = jar.getManifest();
            if (m.getMainAttributes().getValue(new Attributes.Name(SubsystemConstants.SUBSYSTEM_MANIFESTVERSION)) != null
                && m.getMainAttributes().getValue(new Attributes.Name(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME)) != null
                && m.getMainAttributes().getValue(new Attributes.Name(SubsystemConstants.SUBSYSTEM_VERSION)) != null)
            {
                return true;
            }
        }
        catch (Exception e)
        {
            // Ignore
        }
        finally
        {
            if (jar != null)
            {
                try
                {
                    jar.close();
                }
                catch (IOException e)
                {
                    // Ignore
                }
            }
        }
        return false;
    }

}
