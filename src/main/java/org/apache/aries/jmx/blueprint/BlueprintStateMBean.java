/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.jmx.blueprint;

import java.io.IOException;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

/**
 * This MBean provides the management interface to the OSGi Blueprint Service.
 *
 * This MBean also emits events that clients can use to get notified of the
 * changes in the blueprint containers state in the framework.
 *
 * @version $Revision$
 */
public interface BlueprintStateMBean {
    /**
     * The object name for this MBean.
     */
    String OBJECTNAME = JmxConstants.ARIES_BLUEPRINT+":service=blueprintState,version=1.0";

    ///////////////////////////////////////////////////////////////
    // Define Event's CompositeType
    ///////////////////////////////////////////////////////////////

    /**
     * The key BUNDLE_ID, used in {@link #BUNDLE_ID_ITEM}.
     */
    String          BUNDLE_ID                  = "BundleId";

    /**
     * The item containing the Blueprint bundle id associated with this event.
     * The key is {@link #BUNDLE_ID}, and the type is {@link SimpleType#LONG}.
     */
    Item            BUNDLE_ID_ITEM             = new Item(
                                                    BUNDLE_ID,
                                                    "the Blueprint bundle id associated with this event.",
                                                    SimpleType.LONG);

    /**
     * The key EXTENDER_BUNDLE_ID, used in {@link #EXTENDER_BUNDLE_ID_ITEM}.
     */
    String          EXTENDER_BUNDLE_ID   = "ExtenderBundleId";

    /**
     * The item containing the Blueprint extender bundle id that is generating this event.
     * The key is {@link #EXTENDER_BUNDLE_ID}, and the type is {@link SimpleType#LONG}.
     */
    Item            EXTENDER_BUNDLE_ID_ITEM    = new Item(
                                                    EXTENDER_BUNDLE_ID,
                                                    "the Blueprint extender bundle id that is generating this event.",
                                                    SimpleType.LONG);

    /**
     * The key REPLAY, used in {@link #REPLAY_ITEM}.
     */
    String          REPLAY               = "Replay";

    /**
     * The item containing the flag that represents whether this event is a replay event.
     * The key is {@link #REPLAY}, and the type is {@link SimpleType#BOOLEAN}.
     */
    Item            REPLAY_ITEM          = new Item(
                                                    REPLAY,
                                                    "the flag that represents whether this event is a replay event.",
                                                    SimpleType.BOOLEAN);


    /**
     * The key EVENT_TYPE, used in {@link #EVENT_TYPE_ITEM}.
     */
    String          EVENT_TYPE                    = "EventType";

    /**
     * The item containing the type of this event.
     * The key is {@link #EVENT_TYPE}, and the type is {@link SimpleType#STRING}.
     */
    Item            EVENT_TYPE_ITEM              = new Item(
                                                    EVENT_TYPE,
                                                    "The type of the event: {CREATING=1, CREATED=2, DESTROYING=3, DESTROYED=4, FAILURE=5, GRACE_PERIOD=6, WAITING=7}",
                                                    SimpleType.INTEGER);

    /**
     * The key TIMESTAMP, used in {@link #TIMESTAMP_ITEM}.
     */
    String          TIMESTAMP               = "Timestamp";

    /**
     * The item containing the time at which this event was created.
     * The key is {@link #TIMESTAMP}, and the type is {@link SimpleType#LONG}.
     */
    Item            TIMESTAMP_ITEM          = new Item(
                                                    TIMESTAMP,
                                                    "the time at which this event was created.",
                                                    SimpleType.LONG);


    /**
     * The key DEPENDENCIES, used in {@link #DEPENDENCIES_ITEM}.
     */
    String          DEPENDENCIES            = "Dependencies";

    /**
     * The item containing the filters identifying the missing dependencies that caused the WAITING, GRACE_PERIOD or FAILURE event.
     * The key is {@link #DEPENDENCIES}, and the type is {@link JmxConstants#STRING_ARRAY_TYPE}.
     */
    Item            DEPENDENCIES_ITEM       = new Item(
                                                    DEPENDENCIES,
                                                    "the filters identifying the missing dependencies that caused the WAITING, GRACE_PERIOD or FAILURE event.",
                                                    JmxConstants.STRING_ARRAY_TYPE);

    /**
     * The key EXCEPTION_MESSAGE, used in {@link #EXCEPTION_MESSAGE_ITEM}.
     */
    String          EXCEPTION_MESSAGE       = "ExceptionMessage";

    /**
     * The item containing the exception message that cause this FAILURE event.
     * The key is {@link #EXCEPTION_MESSAGE}, and the type is {@link SimpleType#STRING}.
     */
    Item            EXCEPTION_MESSAGE_ITEM  = new Item(
                                                    EXCEPTION_MESSAGE,
                                                    "the exception message that cause this FAILURE event.",
                                                    SimpleType.STRING);

    /**
     * The CompositeType for a blueprint event. It contains the following items:
     * <ul>
     * <li>{@link #BUNDLE_ID}</li>
     * <li>{@link #EXTENDER_BUNDLE_ID}</li>
     * <li>{@link #EVENT_TYPE}</li>
     * <li>{@link #REPLAY}</li>
     * <li>{@link #TIMESTAMP}</li>
     * <li>{@link #DEPENDENCIES}</li>
     * <li>{@link #EXCEPTION_MESSAGE}</li>
     * </ul>
     */
    CompositeType   OSGI_BLUEPRINT_EVENT_TYPE   = Item.compositeType(
                                                    "OSGI_BLUEPRINT_EVENT",
                                                    "Blueprint event",
                                                    BUNDLE_ID_ITEM,
                                                    EXTENDER_BUNDLE_ID_ITEM,
                                                    EVENT_TYPE_ITEM,
                                                    REPLAY_ITEM,
                                                    TIMESTAMP_ITEM,
                                                    DEPENDENCIES_ITEM,
                                                    EXCEPTION_MESSAGE_ITEM);


    /**
     * The Tabular Type for A list of blueprint events. The row type is
     * {@link #OSGI_BLUEPRINT_EVENT_TYPE}.
     */
    TabularType     OSGI_BLUEPRINT_EVENTS_TYPE  = Item.tabularType(
                                                    "BUNDLES",
                                                    "A list of blueprint events",
                                                    OSGI_BLUEPRINT_EVENT_TYPE,
                                                    new String[] { BUNDLE_ID });

    /**
     * Returns the BlueprintEvent associated with this blueprint container.
     * The returned Composite Data is typed by {@link #OSGI_BLUEPRINT_EVENT_TYPE}.
     *
     * @param bundleId The bundle id of a blueprint bundle
     * @return the last event associated with the blueprint bundle, see {@link #OSGI_BLUEPRINT_EVENT_TYPE}
     * @throws IOException if the operation fails
     * @throws IllegalArgumentException if the bundle is not a blueprint bundle
     */
    public CompositeData getLastEvent(long bundleId) throws IOException;

    /**
     * Returns all the last events associated with the blueprint bundles.
     *
     * @return the tabular representation of all the last events associated with the blueprint bundles see {@link #OSGI_BLUEPRINT_EVENTS_TYPE}
     * @throws IOException if the operation fails
     */
    public TabularData getLastEvents() throws IOException;

    /**
     * Returns all the blueprint bundles' IDs, which are either
     * successfully created or not by current extender.
     *
     * @return the list of all the blueprint bundles's IDs (either successfully created or not by current extender)
     * @throws IOException if the operation fails
     */
    public long[] getBlueprintBundleIds() throws IOException;

}
