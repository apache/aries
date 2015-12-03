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
package org.apache.aries.subsystem.core.archive;

/**
 * Aries specific implementation directive that can be added to the
 * SubsystemSymbolicName header to defer provision of dependencies to start time
 * (refer to jira aries-1383 for fuller description of this behavior).
 * <p>
 * The legal values for the directives are "start" and "install". The default is
 * install.
 * <p>
 * There are exactly two instances of this type constructed. Statically
 * references are
 * {@code AriesProvisionDependenciesDirective.PROVISION_DEPENDENCIES_AT_INSTALL}
 * or
 * {@code AriesProvisionDependenciesDirective.PROVISION_DEPENDENCIES_AT_START}.
 */
public class AriesProvisionDependenciesDirective extends AbstractDirective {
    public static final String NAME = "apache-aries-provision-dependencies";
    
    public static final String VALUE_INSTALL = "install";
    public static final String VALUE_RESOLVE = "resolve";

    public static final AriesProvisionDependenciesDirective INSTALL = new AriesProvisionDependenciesDirective(VALUE_INSTALL);
    public static final AriesProvisionDependenciesDirective RESOLVE = new AriesProvisionDependenciesDirective(VALUE_RESOLVE);

    public static final AriesProvisionDependenciesDirective DEFAULT = INSTALL;

    public static AriesProvisionDependenciesDirective getInstance(String value) {
        if (VALUE_INSTALL.equals(value))
            return INSTALL;
        if (VALUE_RESOLVE.equals(value))
            return RESOLVE;
        throw new IllegalArgumentException(value);
    }

    private AriesProvisionDependenciesDirective(String value) {
        super(NAME, value);
    }

    public String getProvisionDependencies() {
        return getValue();
    }

    public boolean isInstall() {
        return this == INSTALL;
    }

    public boolean isResolve() {
        return this == RESOLVE;
    }
}
