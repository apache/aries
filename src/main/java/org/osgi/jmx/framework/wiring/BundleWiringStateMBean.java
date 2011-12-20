/*
 * Copyright (c) OSGi Alliance (2010, 2011). All Rights Reserved.
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
package org.osgi.jmx.framework.wiring;

import java.io.IOException;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

import org.osgi.jmx.Item;
import org.osgi.jmx.JmxConstants;

/**
 * This MBean represents the bundle wiring state.
 *
 * Note that not all information from the BundleWiring Java API is provided.
 *
 * Particularly, the limitations are:
 *  - Cannot retrieve references to resources (e.g. class) of a particular bundle wiring.
 */
public interface BundleWiringStateMBean {
    /*
     * The Object Name for a Bundle Revisions MBean.
     */
    String OBJECTNAME = JmxConstants.OSGI_CORE
        + ":type=wiringState,version=1.0";

    /*
     * Namespaces
     *
     */
    String BUNDLE_NAMESPACE = "osgi.wiring.bundle";
    String HOST_NAMESPACE = "osgi.wiring.host";
    String PACKAGE_NAMESPACE = "osgi.wiring.package";

    /*
     * Items, CompositeData, TabularData, ArrayTypes
     *
     */
    String KEY = "Key";
    Item KEY_ITEM = new Item(KEY, "The directive key", SimpleType.STRING);

    String VALUE = "Value";
    Item VALUE_ITEM = new Item(VALUE, "The directive value",
            SimpleType.STRING);

    CompositeType DIRECTIVE_TYPE = Item.compositeType("DIRECTIVE",
            "Describes a directive of a capability or requirement",
            KEY_ITEM, VALUE_ITEM);
    TabularType DIRECTIVES_TYPE = Item.tabularType("DIRECTIVES",
            "Describes the directives of a capability or requirement",
            DIRECTIVE_TYPE, KEY
            );

    String DIRECTIVES = "Directives";
    Item DIRECTIVES_ITEM = new Item(DIRECTIVES,
            "The directives of a capability or requirement",
            DIRECTIVES_TYPE);

    // REVIEW should we reuse from JmxConstants here or create our own?
    TabularType ATTRIBUTES_TYPE = Item.tabularType("ATTRIBUTES",
            "Describes attributes of a capability or requirement",
            JmxConstants.PROPERTY_TYPE, JmxConstants.KEY
            );
    String ATTRIBUTES = "Attributes";
    Item ATTRIBUTES_ITEM = new Item(ATTRIBUTES,
            "The attributes of a capability or requirement",
            ATTRIBUTES_TYPE);

    String NAMESPACE = "Namespace";
    Item NAMESPACE_ITEM = new Item(NAMESPACE,
            "The namespace of a capability or requirement",
            SimpleType.STRING);

    CompositeType BUNDLE_REQUIREMENT_TYPE =
        Item.compositeType("BUNDLE_REQUIREMENT",
                "Describes the live wired requirements of a bundle",
                ATTRIBUTES_ITEM, DIRECTIVES_ITEM, NAMESPACE_ITEM);

    CompositeType BUNDLE_CAPABILITY_TYPE =
        Item.compositeType("BUNDLE_CAPABILITY",
                "Describes the live wired capabilities of a bundle",
                ATTRIBUTES_ITEM, DIRECTIVES_ITEM, NAMESPACE_ITEM);

    String PROVIDER_BUNDLE_ID = "ProviderBundleId";
    Item PROVIDER_BUNDLE_ID_ITEM = new Item(PROVIDER_BUNDLE_ID,
            "The identifier of the bundle that is the provider of the capability",
            SimpleType.LONG);

    String REQUIRER_BUNDLE_ID = "RequirerBundleId";
    Item REQUIRER_BUNDLE_ID_ITEM = new Item(REQUIRER_BUNDLE_ID,
            "The identifier of the bundle that is the requirer of the requirement",
            SimpleType.LONG);

    String BUNDLE_REQUIREMENT = "BundleRequirement";
    Item BUNDLE_REQUIREMENT_ITEM = new Item(BUNDLE_REQUIREMENT,
            "The wired requirements of a bundle",
            BUNDLE_REQUIREMENT_TYPE);

    String BUNDLE_CAPABILITY = "BundleCapability";
    Item BUNDLE_CAPABILITY_ITEM = new Item(BUNDLE_CAPABILITY,
            "The wired capabilities of a bundle",
            BUNDLE_CAPABILITY_TYPE);

    String PROVIDER_BUNDLE_REVISION_ID = "ProviderBundleRevisionId";
    Item PROVIDER_BUNDLE_REVISION_ID_ITEM = new Item(PROVIDER_BUNDLE_REVISION_ID,
            "A local id for the bundle revision that is the provider of the capability",
            SimpleType.INTEGER);

    String REQUIRER_BUNDLE_REVISION_ID = "RequirerBundleRevisionId";
    Item REQUIRER_BUNDLE_REVISION_ID_ITEM =  new Item(REQUIRER_BUNDLE_REVISION_ID,
            "A local id for the bundle revision that is the requirer of the requirement",
            SimpleType.INTEGER);

    /**
     * Describes the live association between a provider of
     *  a capability and a requirer of the corresponding requirement.
     */
    CompositeType BUNDLE_WIRE_TYPE =
        Item.compositeType("BUNDLE_WIRE",
                "Describes the live association between a provider and a requirer",
                BUNDLE_REQUIREMENT_ITEM,
                BUNDLE_CAPABILITY_ITEM,
                PROVIDER_BUNDLE_ID_ITEM,
                PROVIDER_BUNDLE_REVISION_ID_ITEM,
                REQUIRER_BUNDLE_ID_ITEM,
                REQUIRER_BUNDLE_REVISION_ID_ITEM
                );
    ArrayType BUNDLE_WIRES_TYPE_ARRAY =
        Item.arrayType(1, BUNDLE_WIRE_TYPE);

    String BUNDLE_REVISION_ID = "BundleRevisionId";
    Item BUNDLE_REVISION_ID_ITEM = new Item(BUNDLE_REVISION_ID,
            "The local identifier of the bundle revision",
            SimpleType.INTEGER);

    String BUNDLE_ID = "BundleId";
    Item BUNDLE_ID_ITEM = new Item(BUNDLE_ID,
            "The bundle identifier of the bundle revision",
            SimpleType.LONG);

    ArrayType REQUIREMENT_TYPE_ARRAY =
        Item.arrayType(1, BUNDLE_REQUIREMENT_TYPE);
    ArrayType CAPABILITY_TYPE_ARRAY =
        Item.arrayType(1, BUNDLE_CAPABILITY_TYPE);

    String REQUIREMENTS = "Requirements";
    Item REQUIREMENTS_ITEM = new Item(REQUIREMENTS,
            "The bundle requirements of a bundle revision wiring",
            REQUIREMENT_TYPE_ARRAY);

    CompositeType REVISION_REQUIREMENTS_TYPE =
        Item.compositeType("REVISION_REQUIREMENTS",
            "Describes the requirements for a bundle revision",
            BUNDLE_ID_ITEM,
            BUNDLE_REVISION_ID_ITEM,
            REQUIREMENTS_ITEM);

    TabularType REVISIONS_REQUIREMENTS_TYPE =
        Item.tabularType("REVISIONS_REQUIREMENTS",
            "The bundle requirements for all bundle revisions",
            REVISION_REQUIREMENTS_TYPE,
            BUNDLE_ID, BUNDLE_REVISION_ID);

    String CAPABILITIES = "Capabilities";
    Item CAPABILITIES_ITEM = new Item(CAPABILITIES,
            "The bundle capabilities of a bundle revision wiring",
            CAPABILITY_TYPE_ARRAY);

    CompositeType REVISION_CAPABILITIES_TYPE =
        Item.compositeType("REVISION_CAPABILITIES",
            "Describes the capabilities for a bundle revision",
            BUNDLE_ID_ITEM,
            BUNDLE_REVISION_ID_ITEM,
            CAPABILITIES_ITEM);

    TabularType REVISIONS_CAPABILITIES_TYPE =
        Item.tabularType("REVISIONS_CAPABILITIES",
            "The bundle capabilities for all bundle revisions",
            REVISION_CAPABILITIES_TYPE,
            BUNDLE_ID, BUNDLE_REVISION_ID);

    String PROVIDED_WIRES = "ProvidedWires";
    Item PROVIDED_WIRES_ITEM = new Item(PROVIDED_WIRES,
            "The bundle wires to the capabilities provided by this bundle wiring.",
            BUNDLE_WIRES_TYPE_ARRAY);

    String REQUIRED_WIRES = "RequiredWires";
    Item REQUIRED_WIRES_ITEM = new Item(REQUIRED_WIRES,
            "The bundle wires to requirements in use by this bundle wiring",
            BUNDLE_WIRES_TYPE_ARRAY);

    CompositeType BUNDLE_WIRING_TYPE =
        Item.compositeType("BUNDLE_WIRING",
                "Describes the runtime association between a provider and a requirer",
                BUNDLE_ID_ITEM,               /* Long */
                BUNDLE_REVISION_ID_ITEM,      /* Integer (local scope) */
                REQUIREMENTS_ITEM,            /* REQUIREMENT_TYPE [] */
                CAPABILITIES_ITEM,            /* CAPABILITIES_TYPE [] */
                REQUIRED_WIRES_ITEM,          /* BUNDLE_WIRE_TYPE [] */
                PROVIDED_WIRES_ITEM           /* BUNDLE_WIRE_TYPE [] */
                );
    TabularType REVISIONS_BUNDLE_WIRING_TYPE =
        Item.tabularType("REVISIONS_BUNDLE_WIRING",
            "The bundle wiring for all bundle revisions",
            BUNDLE_WIRING_TYPE,
            BUNDLE_ID, BUNDLE_REVISION_ID);
    /*
    ArrayType BUNDLE_WIRING_TYPE_ARRAY =
        Item.arrayType(1, BUNDLE_WIRING_TYPE);

    ArrayType REVISIONS_REQUIREMENT_TYPE_ARRAY =
        Item.arrayType(2, BUNDLE_REQUIREMENT_TYPE);

    ArrayType REVISIONS_CAPABILITY_TYPE_ARRAY =
        Item.arrayType(2, BUNDLE_CAPABILITY_TYPE);
    */

    TabularType BUNDLE_WIRING_CLOSURE_TYPE = Item.tabularType("BUNDLE_WIRING_CLOSURE",
            "A table of bundle wirings describing a full wiring closure",
            BUNDLE_WIRING_TYPE,
            BUNDLE_ID, BUNDLE_REVISION_ID);

    /**
     * Returns the requirements for the current bundle revision.
     * The ArrayType is typed by the {@link #REQUIREMENT_TYPE_ARRAY}.
     *
     * @param bundleId
     * @param namespace
     * @return the declared requirements for the current revision of <code>bundleId</code>
     * and <code>namespace</code>
     *
     */
    CompositeData[] getCurrentRevisionDeclaredRequirements(long bundleId,
            String namespace) throws IOException;

    /**
     * Returns the capabilities for the current bundle revision.
     * The ArrayType is typed by the {@link #CAPABILITY_TYPE_ARRAY}
     *
     * @param bundleId
     * @param namespace
     * @return the declared capabilities for the current revision of <code>bundleId</code>
     * and <code>namespace</code>
     */
    CompositeData[] getCurrentRevisionDeclaredCapabilities(long bundleId,
            String namespace) throws IOException;

    /**
     * Returns the bundle wiring for the current bundle revision.
     * The ArrayType is typed by the {@link #BUNDLE_WIRING_TYPE}
     *
     * @param bundleId
     * @param namespace
     * @return the wires for the current revision of <code>bundleId</code>
     * and <code>namespace</code>
     */
    CompositeData getCurrentWiring(long bundleId, String namespace) throws IOException;

    /**
     * Returns the bundle wiring closure for the current revision of the specified bundle.
     *
     * @see #BUNDLE_WIRING_CLOSURE_TYPE for the details of the TabularType
     *
     * @param rootBundleId the root bundle of the closure.
     * @param namespace the namespace of the capabilities and requirements involved in this wiring.
     * @return a tabular representation of all the wiring in the closure. The bundle revision ids
     * only have meaning in the context of the current result. The revision of the rootBundle is set
     * to 0.
     */
    TabularData getCurrentWiringClosure(long rootBundleId, String namespace) throws IOException;

    /**
     * Returns the requirements for all revisions of the bundle.
     * The ArrayType is typed by the {@link #REVISIONS_REQUIREMENT_TYPE_ARRAY}.
     * The requirements are in no particular order, and may change in
     *  subsequent calls to this operation.
     *
     * @param bundleId
     * @param namespace
     * @return the declared requirements for all revisions of <code>bundleId</code>
     *
     */
    TabularData getRevisionsDeclaredRequirements(long bundleId,
            String namespace) throws IOException;

    /**
     * Returns the capabilities for all revisions of the bundle.
     * The ArrayType is typed by the {@link #REVISIONS_CAPABILITY_TYPE_ARRAY}
     * The capabilities are in no particular order, and may change in
     *  subsequent calls to this operation.
     *
     * @param bundleId
     * @param namespace
     * @return the declared capabilities for all revisions of <code>bundleId</code>
     */
    TabularData getRevisionsDeclaredCapabilities(long bundleId,
            String namespace) throws IOException;

    /**
     * Returns the bundle wirings for all revisions of the bundle.
     * The ArrayType is typed by the {@link #BUNDLE_WIRING_TYPE_ARRAY}
     * The bundle wirings are in no particular order, and may
     *  change in subsequent calls to this operations.
     *
     * @param bundleId
     * @param namespace
     * @return the wires for all revisions of <code>bundleId</code>
     */
    TabularData getRevisionsWiring(long bundleId, String namespace) throws IOException;

    /**
     * Returns a closure of all bundle wirings linked by their
     *  bundle wires, starting at <code>rootBundleId</code>.
     * The ArrayType is typed by the {@link #BUNDLE_WIRING_TYPE_ARRAY}
     * The bundle wirings are in no particular order, and may
     *  change in subsequent calls to this operation. Furthermore,
     * the bundle wiring IDs are local and cannot be reused across invocations.
     *
     * @param rootBundleId
     * @param namespace
     * @return a closure of bundle wirings linked together by wires.
     */
    TabularData getWiringClosure(long rootBundleId, String namespace) throws IOException;
}