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

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.SimpleType;


public interface BlueprintMetadataMBean {

    /**
     * The object name for this MBean.
     */
    String OBJECTNAME = JmxConstants.ARIES_BLUEPRINT+":service=blueprintMetadata,version=1.0";
    
    
    ///////////////////////////////////////////////////////////////
    // Define <value>'s CompositeType
    ///////////////////////////////////////////////////////////////
    
    /**
     * The key STRING_VALUE, used in {@link #STRING_VALUE_ITEM}.
     */
    String          STRING_VALUE            = "StringValue";
    
    /**
     * The item containing the un-converted string representation of the value.
     * The key is {@link #STRING_VALUE}, and the type is {@link SimpleType#STRING}.
     */
    Item            STRING_VALUE_ITEM       = new Item(
                                                    STRING_VALUE, 
                                                    "The un-converted string representation of a value", 
                                                    SimpleType.STRING);
    
    /**
     * The key TYPE, used in {@link #TYPE_ITEM}.
     */
    String          TYPE                    = "Type";
    
    /**
     * The item containing the name of the type to which the value should be converted.
     * The key is {@link #TYPE}, and the type is {@link SimpleType#STRING}.
     */
    Item            TYPE_ITEM               = new Item(
                                                    TYPE, 
                                                    "The type of a value", 
                                                    SimpleType.STRING);
    
    /**
     * The name of CompositeType for ValueMetadata objects, used in {@link #VALUE_METADATA_TYPE}.
     */
    String          VALUE_METADATA          = "ValueMetadata";
    
    
    /**
     * The CompositeType encapsulates ValueMetadata objects. It contains the following items:
     * <ul>
     * <li>{@link #STRING_VALUE}</li>
     * <li>{@link #TYPE}</li>
     * </ul>
     */
    CompositeType   VALUE_METADATA_TYPE     = Item.compositeType(
                                                    VALUE_METADATA, 
                                                    "This type encapsulates ValueMetadata objects", 
                                                    STRING_VALUE_ITEM, 
                                                    TYPE_ITEM);                  
    
    ///////////////////////////////////////////////////////////////
    // Define <null>'s CompositeType
    ///////////////////////////////////////////////////////////////  
    
    /**
     * The key PLACEHOLDER, used in {@link #PLACEHOLDER_ITEM}.
     */ 
    String          PLACEHOLDER             = "Placeholder";
    
    /**
     * The item is a placeholder in the null metadata type.
     * The key is {@link #PLACEHOLDER}, and the type is {@link SimpleType#STRING}.
     */
    Item            PLACEHOLDER_ITEM        = new Item(
                                                    PLACEHOLDER, 
                                                    "This is a placeholder", 
                                                    SimpleType.VOID);
    
    /**
     * The name of CompositeType for NullMetadata objects, used in {@link #NULL_METADATA_TYPE}.
     */
    String          NULL_METADATA          = "NullMetadata";
    
    /**
     * The CompositeType for NullMetadata objects. A composite type requires at least one item, so we add a placeholder item.
     */
    CompositeType   NULL_METADATA_TYPE      = Item.compositeType(
                                                    NULL_METADATA, 
                                                    "This type encapsulates NullMetadata objects", 
                                                    PLACEHOLDER_ITEM);
    
    ///////////////////////////////////////////////////////////////
    // Define <ref>'s CompositeType
    ///////////////////////////////////////////////////////////////      
    
    /**
     * The key COMPONENT_ID, used in {@link #COMPONENT_ID_ITEM}.
     */
    String          COMPONENT_ID            = "ComponentId";
    
    /**
     * The item containing the component id to which the "ref" associates.
     * The key is {@link #COMPONENT_ID}, and the type is {@link SimpleType#STRING}.
     */
    Item            COMPONENT_ID_ITEM       = new Item(
                                                    COMPONENT_ID,
                                                    "The component id",
                                                    SimpleType.STRING);
    /**
     * The name of CompositeType for RefMetadata objects, used in {@link #REF_METADATA_TYPE}.
     */
    String          REF_METADATA          = "RefMetadata";
    
    /**
     * The CompositeType for a RefMetadata object. It contains the following items:
     * <ul>
     * <li>{@link #COMPONENT_ID}</li>
     * </ul>
     */
    CompositeType   REF_METADATA_TYPE       = Item.compositeType(
                                                    REF_METADATA, 
                                                    "This type encapsulates RefMetadata objects", 
                                                    COMPONENT_ID_ITEM);

    ///////////////////////////////////////////////////////////////
    // Define <idref>'s CompositeType
    // COMPONENT_ID_ITEM defined in <ref>'s definition
    ///////////////////////////////////////////////////////////////  
    /**
     * The name of CompositeType for IdRefMetadata objects, used in {@link #ID_REF_METADATA_TYPE}.
     */
    String          ID_REF_METADATA          = "IdRefMetadata";
    
    /**
     * The CompositeType for an IdRefMetadata object. It contains the following items:
     * <ul>
     * <li>{@link #COMPONENT_ID}</li>
     * </ul>
     */
    CompositeType   ID_REF_METADATA_TYPE    = Item.compositeType(
                                                    ID_REF_METADATA, 
                                                    "This type encapsulates IdRefMetadata objects", 
                                                    COMPONENT_ID_ITEM);
    
    ///////////////////////////////////////////////////////////////
    // Define <entry>'s CompositeType, 
    // used by MapMetadata, PropsMetadata, and Service properties
    ///////////////////////////////////////////////////////////////    
    
    /**
     * The key KEY, used in {@link #KEY_ITEM}.
     */    
    String          KEY                     = "Key";
    
    /**
     * The item containing the key of an entry.
     * The key is {@link #KEY}, and the type is {@link SimpleType#STRING}.
     */
    Item            KEY_ITEM                = new Item(
                                                    KEY,
                                                    "The key of an entry",
                                                    JmxConstants.BYTE_ARRAY_TYPE);
    
    /**
    * The key VALUE, used in {@link #VALUE_ITEM}.
    */
    String          VALUE                   = "Value";
       
    /**
    * The item containing a value and this will be used by 
    * BeanArgument, BeanProperty, MapEntry and CollectionMetadata.
    * The key is {@link #VALUE}, and the type is {@link JmxConstants#BYTE_ARRAY_TYPE}.
    */
    Item            VALUE_ITEM              = new Item(
                                                   VALUE, 
                                                   "A value", 
                                                   JmxConstants.BYTE_ARRAY_TYPE);  

    /**
     * The name of CompositeType for MapEntry objects, used in {@link #MAP_ENTRY_TYPE}.
     */
    String          MAP_ENTRY          = "MapEntry";
    
    /**
     * The CompositeType for a MapEntry object. It contains the following items:
     * <ul>
     * <li>{@link #KEY}</li>
     * <li>{@link #VALUE}</li>
     * </ul>
     */
    CompositeType   MAP_ENTRY_TYPE          = Item.compositeType(
                                                   MAP_ENTRY, 
                                                   "This type encapsulates MapEntry objects",
                                                   KEY_ITEM,
                                                   VALUE_ITEM);
    
    ///////////////////////////////////////////////////////////////
    // Define <map>'s CompositeType
    ///////////////////////////////////////////////////////////////
    
    /**
     * The key ENTRIES, used in {@link #ENTRIES_ITEM}.
     */
    String          ENTRIES                 = "Entries";
    
    /**
     * The item containing an array of entries
     * The key is {@link #ENTRIES}, and the type is {@link ArrayType}.
     */
    Item            ENTRIES_ITEM            = new Item(
                                                    ENTRIES,
                                                    "An array of entries",
                                                    Item.arrayType(1, MAP_ENTRY_TYPE));
    
    /**
     * The key KEY_TYPE, used in {@link #KEY_TYPE_ITEM}.
     */
    String          KEY_TYPE                = "KeyType";
    
    /**
     * The item containing the key type of the entries.
     * The key is {@link #KEY_TYPE}, and the type is {@link SimpleType#STRING}.
     */
    Item            KEY_TYPE_ITEM           = new Item(
                                                    KEY_TYPE,
                                                    "The key type of the entries",
                                                    SimpleType.STRING);
    
    /**
     * The key VALUE_TYPE, used in {@link #VALUE_TYPE_ITEM}.
     */
    String          VALUE_TYPE              = "ValueType";
    
    /**
     * The item containing the value type that the value should be
     * The key is {@link #VALUE_TYPE}, and the type is {@link SimpleType#STRING}.
     */
    Item            VALUE_TYPE_ITEM         = new Item(
                                                    VALUE_TYPE, 
                                                    "The value type", 
                                                    SimpleType.STRING);
    
    /**
     * The name of CompositeType for MapMetadata objects, used in {@link #MAP_METADATA_TYPE}.
     */
    String          MAP_METADATA          = "MapMetadata";
    
    /**
     * The CompositeType for a MapMetadata object. It contains the following items:
     * <ul>
     * <li>{@link #ENTRIES}</li>
     * <li>{@link #KEY_TYPE}</li>
     * <li>{@link #VALUE_TYPE}</li>
     * </ul>
     */
    CompositeType   MAP_METADATA_TYPE       = Item.compositeType(
                                                    MAP_METADATA, 
                                                    "This type encapsulates MapMetadata objects",
                                                    ENTRIES_ITEM,
                                                    KEY_TYPE_ITEM,
                                                    VALUE_TYPE_ITEM);
    
    ///////////////////////////////////////////////////////////////
    // Define <props>'s CompositeType
    // ENTRIES_ITEM defined in <map>'s definition
    ///////////////////////////////////////////////////////////////

    /**
     * The name of CompositeType for PropsMetadata objects, used in {@link #PROPS_METADATA_TYPE}.
     */
    String          PROPS_METADATA          = "PropsMetadata";    
    
    /**
     * The CompositeType for a PropsMetadata object. It contains the following items:
     * <ul>
     * <li>{@link #ENTRIES}</li>
     * </ul>
     */
    CompositeType   PROPS_METADATA_TYPE     = Item.compositeType(
                                                    PROPS_METADATA, 
                                                    "This type encapsulates PropsMetadata objects",
                                                    ENTRIES_ITEM);
    
    ///////////////////////////////////////////////////////////////
    // Define <collection>'s CompositeType
    // VALUE_TYPE_ITEM defined in <map>'s definition
    ///////////////////////////////////////////////////////////////   
    
    /**
     * The key COLLECTION_CLASS, used in {@link #KEY_TYPE_ITEM}.
     */
    String          COLLECTION_CLASS        = "CollectionClass";    
    
    /**
     * The item containing the type of this collection
     * The key is {@link #COLLECTION_CLASS}, and the type is {@link SimpleType#STRING}.
     */
    Item            COLLECTION_CLASS_ITEM   = new Item(
                                                    COLLECTION_CLASS,
                                                    "The type of this collection",
                                                    SimpleType.STRING);
    
    /**
     * The key VALUES, used in {@link #VALUES_ITEM}.
     */
    String          VALUES                  = "Values";
    
    /**
     * The item containing all the values
     * The key is {@link #VALUES}, and the type is {@link ArrayType}.
     */
    Item            VALUES_ITEM             = new Item(
                                                    VALUES,
                                                    "All the values",
                                                    Item.arrayType(2, SimpleType.BYTE));
    

    /**
     * The name of CompositeType for CollectionMetadata objects, used in {@link #COLLECTION_METADATA_TYPE}.
     */
    String          COLLECTION_METADATA          = "CollectionMetadata";    
    
    /**
     * The CompositeType for a CollectionMetadata object. It contains the following items:
     * <ul>
     * <li>{@link #COLLECTION_CLASS}</li>
     * <li>{@link #VALUES}</li>
     * <li>{@link #VALUE_TYPE}</li>
     * </ul>
     */
    CompositeType   COLLECTION_METADATA_TYPE= Item.compositeType(
                                                    COLLECTION_METADATA, 
                                                    "This type encapsulates CollectionMetadata objects",
                                                    COLLECTION_CLASS_ITEM,
                                                    VALUES_ITEM,
                                                    VALUE_TYPE_ITEM);
   
    ///////////////////////////////////////////////////////////////
    // Define <argument>'s CompositeType
    // VALUE_TYPE_ITEM defined in <map>'s definition
    ///////////////////////////////////////////////////////////////
    
    /**
     * The key INDEX, used in {@link #INDEX_ITEM}.
     */
    String          INDEX                   = "Index";
    
    /**
     * The item containing the zero-based index into the parameter list of
     * the factory method or constructor to be invoked for this argument.
     * The key is {@link #INDEX}, and the type is {@link SimpleType#INTEGER}.
     */
    Item            INDEX_ITEM              = new Item(
                                                    INDEX, 
                                                    "The zero-based index", 
                                                    SimpleType.INTEGER);

    /**
     * The name of CompositeType for BeanArgument objects, used in {@link #BEAN_ARGUMENT_TYPE}.
     */
    String          BEAN_ARGUMENT               = "BeanArgument";    

    
    /**
     * The CompositeType for a Argument object. It contains the following items:
     * <ul>
     * <li>{@link #INDEX}</li>
     * <li>{@link #VALUE_TYPE}</li>
     * <li>{@link #VALUE}</li>
     * </ul>
     */
    CompositeType   BEAN_ARGUMENT_TYPE          = Item.compositeType(
                                                    BEAN_ARGUMENT,
                                                    "This type encapsulates BeanArgument objects",
                                                    INDEX_ITEM,
                                                    VALUE_TYPE_ITEM,
                                                    VALUE_ITEM);
       
    ///////////////////////////////////////////////////////////////
    // Define <property>'s CompositeType
    ///////////////////////////////////////////////////////////////
    
    /**
     * The key NAME, used in {@link #NAME_ITEM}.
     */
    String          NAME                    = "Name";
    
    /**
     * The item containing the name of the property to be injected.
     * The key is {@link #NAME}, and the type is {@link SimpleType#STRING}.
     */
    Item            NAME_ITEM               = new Item(
                                                    NAME,
                                                    "The name of the property",
                                                    SimpleType.STRING);
    
    /**
     * The name of CompositeType for BeanProperty objects, used in {@link #BEAN_PROPERTY_TYPE}.
     */
    String          BEAN_PROPERTY               = "BeanProperty";    
    
    /**
     * The CompositeType for property type. It contains the following items:
     * <ul>
     * <li>{@link #NAME}</li>
     * <li>{@link #VALUE}</li>
     * </ul>
     */
    CompositeType   BEAN_PROPERTY_TYPE           = Item.compositeType(
                                                    BEAN_PROPERTY, 
                                                    "This type encapsulates BeanProperty objects",
                                                    NAME_ITEM,
                                                    VALUE_ITEM);
        
    ///////////////////////////////////////////////////////////////
    // Define Component's CompositeType
    // <bean>, <service> & Service Reference's CompositeType will 
    // extend this.
    ///////////////////////////////////////////////////////////////
    
    /**
     * The key ID, used in {@link #ID_ITEM}.
     */
    String          ID                    = "Id";

    /**
     * The item containing the component id of a component. 
     * The key is {@link #ID}, and the type is {@link SimpleType#STRING}.
     */
    Item            ID_ITEM       = new Item(
                                                    ID,
                                                    "The id of the component",
                                                    SimpleType.STRING);
    /**
     * The key ACTIVATION, used in {@link #ACTIVATION_ITEM}.
     */
    String          ACTIVATION              = "Activation";
    
    /**
     * The item containing the activation strategy for a component. 
     * The key is {@link #ACTIVATION}, and the type is {@link SimpleType#INTEGER}.
     */
    Item            ACTIVATION_ITEM         = new Item(
                                                    ACTIVATION,
                                                    "The activation strategy for a component",
                                                    SimpleType.INTEGER);
    
    /**
     * The key DEPENDS_ON, used in {@link #DEPENDS_ON_ITEM}.
     */
    String          DEPENDS_ON              = "DependsOn";
    
    /**
     * The item containing the ids of any components listed in a <code>depends-on</code> attribute for the component. 
     * The key is {@link #DEPENDS_ON}, and the type is {@link JmxConstants#STRING_ARRAY_TYPE}.
     */
    Item            DEPENDS_ON_ITEM         = new Item(
                                                    DEPENDS_ON,
                                                    "The ids of any components listed in a depends-on attribute",
                                                    JmxConstants.STRING_ARRAY_TYPE);
    
    /**
     * The name of CompositeType for ComponentMetadata objects, used in {@link #COMPONENT_METADATA_TYPE}.
     */
    String          COMPONENT_METADATA               = "ComponentMetadata";    
    
    /**
     * The CompositeType for a ComponentMetadata object, it contains 
     * the following items:
     * <ul>
     * <li>{@link #ID}</li>
     * <li>{@link #ACTIVATION}</li>
     * <li>{@link #DEPENDS_ON}</li>
     * </ul>
     */
    CompositeType   COMPONENT_METADATA_TYPE     = Item.compositeType(
                                                    COMPONENT_METADATA,
                                                    "This type encapsulates ComponentMetadata objects",
                                                    ID_ITEM,
                                                    ACTIVATION_ITEM,
                                                    DEPENDS_ON_ITEM);
                            
    ///////////////////////////////////////////////////////////////
    // Define <bean>'s CompositeType
    ///////////////////////////////////////////////////////////////
    
    /**
     * The key CLASS_NAME, used in {@link #CLASS_NAME_ITEM}.
     */
    String          CLASS_NAME              = "ClassName";
    
    /**
     * The item containing the name of the class specified for the bean. 
     * The key is {@link #CLASS_NAME}, and the type is {@link SimpleType#STRING}.
     */
    Item            CLASS_NAME_ITEM         = new Item(
                                                    CLASS_NAME,
                                                    "The name of the class specified for the bean",
                                                    SimpleType.STRING
                                                    );
    
    /**
     * The key INIT_METHOD, used in {@link #INIT_METHOD_ITEM}.
     */
    String          INIT_METHOD             = "InitMethod";
    
    /**
     * The item containing the name of the init method specified for the bean.
     * The key is {@link #INIT_METHOD}, and the type is {@link SimpleType#STRING}.
     */
    Item            INIT_METHOD_ITEM        = new Item(
                                                    INIT_METHOD, 
                                                    "The name of the init method specified for the bean", 
                                                    SimpleType.STRING);
    
    /**
     * The key DESTROY_METHOD, used in {@link #DESTROY_METHOD_ITEM}.
     */
    String          DESTROY_METHOD          = "DestroyMethod";
    
    /**
     * The item containing the name of the destroy method specified for the bean.
     * The key is {@link #DESTROY_METHOD}, and the type is {@link SimpleType#STRING}.
     */
    Item            DESTROY_METHOD_ITEM     = new Item(
                                                    DESTROY_METHOD, 
                                                    "The name of the destroy method specified for the bean", 
                                                    SimpleType.STRING);

    /**
     * The key FACTORY_METHOD, used in {@link #FACTORY_METHOD_ITEM}.
     */
    String          FACTORY_METHOD          = "FactoryMethod";
    
    /**
     * The item containing the name of the factory method specified for the bean.
     * The key is {@link #FACTORY_METHOD}, and the type is {@link SimpleType#STRING}.
     */
    Item            FACTORY_METHOD_ITEM     = new Item(
                                                    FACTORY_METHOD, 
                                                    "The name of the factory method specified for the bean", 
                                                    SimpleType.STRING);
    
    /**
     * The key FACTORY_COMPONENT, used in {@link #FACTORY_COMPONENT_ITEM}.
     */
    String          FACTORY_COMPONENT       = "FactoryComponent";
    
    /**
     * The item containing the id of the factory component on which to invoke the factory method for the bean.
     * The key is {@link #FACTORY_COMPONENT}, and the type is {@link JmxConstants#BYTE_ARRAY_TYPE}.
     */
    Item            FACTORY_COMPONENT_ITEM  = new Item(
                                                    FACTORY_COMPONENT, 
                                                    "The factory component on which to invoke the factory method for the bean", 
                                                    JmxConstants.BYTE_ARRAY_TYPE);
    
    /**
     * The key SCOPE, used in {@link #SCOPE_ITEM}.
     */
    String          SCOPE                   = "Scope";
    
    /**
     * The item containing the scope for the bean.
     * The key is {@link #SCOPE}, and the type is {@link SimpleType#STRING}.
     */
    Item            SCOPE_ITEM              = new Item(
                                                    SCOPE, 
                                                    "The scope for the bean", 
                                                    SimpleType.STRING);

    /**
     * The key ARGUMENT, used in {@link #ARGUMENTS_ITEM}.
     */
    String          ARGUMENTS                   = "Arguments";
        
    /**
     * The item containing the bean argument for the bean's compositeType.
     * The key is {@link #ARGUMENTS}, and the type is {@link #BEAN_ARGUMENT_TYPE}.
     */
    Item           ARGUMENTS_ITEM     = new Item(
                                                    ARGUMENTS, 
                                                    "The bean argument", 
                                                    Item.arrayType(1, BEAN_ARGUMENT_TYPE));
    
    /**
     * The key PROPERTY, used in {@link #PROPERTIES_ITEM}.
     */
    String          PROPERTIES         = "Properties";
        
    /**
     * The item containing the bean property for the bean's compositeType.
     * The key is {@link #PROPERTIES}, and the type is {@link #BEAN_PROPERTY_TYPE}.
     */
    Item            PROPERTIES_ITEM    = new Item(
                                                    PROPERTIES, 
                                                    "The bean property", 
                                                    Item.arrayType(1, BEAN_PROPERTY_TYPE));
    
    /**
     * The name of CompositeType for BeanMetadata objects, used in {@link #BEAN_METADATA_TYPE}.
     */
    String          BEAN_METADATA               = "BeanMetadata";    
    
    /**
     * The CompositeType for a BeanMetadata object, it extends {@link #COMPONENT_METADATA_TYPE} 
     * and adds the following items:
     * <ul>
     * <li>{@link #CLASS_NAME}</li>
     * <li>{@link #INIT_METHOD}</li>
     * <li>{@link #DESTROY_METHOD}</li>
     * <li>{@link #FACTORY_METHOD}</li>
     * <li>{@link #FACTORY_COMPONENT}</li>
     * <li>{@link #SCOPE}</li>
     * <li>{@link #ARGUMENTS}</li>
     * <li>{@link #PROPERTIES}</li>
     * </ul>
     */
    CompositeType   BEAN_METADATA_TYPE      = Item.extend(
                                                    COMPONENT_METADATA_TYPE, 
                                                    BEAN_METADATA, 
                                                    "This type encapsulates BeanMetadata objects",
                                                    CLASS_NAME_ITEM,
                                                    INIT_METHOD_ITEM,
                                                    DESTROY_METHOD_ITEM, 
                                                    FACTORY_METHOD_ITEM,
                                                    FACTORY_COMPONENT_ITEM,
                                                    SCOPE_ITEM,
                                                    ARGUMENTS_ITEM,
                                                    PROPERTIES_ITEM);

    ///////////////////////////////////////////////////////////////
    // Define <registration-listener>'s CompositeType
    ///////////////////////////////////////////////////////////////
    
    /**
     * The key LISTENER_COMPONENT, used in {@link #LISTENER_COMPONENT_ITEM}.
     */
    String          LISTENER_COMPONENT      = "ListenerComponent";
    
    /**
     * The item containing the listener component.
     * The key is {@link #LISTENER_COMPONENT}, and the type is {@link JmxConstants#BYTE_ARRAY_TYPE}.
     */
    Item            LISTENER_COMPONENT_ITEM = new Item(
                                                    LISTENER_COMPONENT, 
                                                    "The listener component", 
                                                    JmxConstants.BYTE_ARRAY_TYPE);
    
    /**
     * The key REGISTRATION_METHOD, used in {@link #REGISTRATION_METHOD_ITEM}.
     */
    String          REGISTRATION_METHOD     = "RegistrationMethod";
    
    /**
     * The item containing the name of the registration method.
     * The key is {@link #REGISTRATION_METHOD}, and the type is {@link SimpleType#STRING}.
     */
    Item            REGISTRATION_METHOD_ITEM    = new Item(
                                                    REGISTRATION_METHOD, 
                                                    "The name of the registration method", 
                                                    SimpleType.STRING);
    
    /**
     * The key UNREGISTRATION_METHOD, used in {@link #UNREGISTRATION_METHOD_ITEM}.
     */
    String          UNREGISTRATION_METHOD       = "UnregistrationMethod";
    
    /**
     * The item containing the name of the unregistration method.
     * The key is {@link #UNREGISTRATION_METHOD}, and the type is {@link SimpleType#STRING}.
     */
    Item            UNREGISTRATION_METHOD_ITEM  = new Item(
                                                    UNREGISTRATION_METHOD, 
                                                    "The name of the unregistration method", 
                                                    SimpleType.STRING);
    
    /**
     * The name of CompositeType for RegistrationListener objects, used in {@link #REGISTRATION_LISTENER_TYPE}.
     */
    String          REGISTRATION_LISTENER               = "RegistrationListener";    
    
    /**
     * The CompositeType for a registration listener, and it contains the following items:
     * <ul>
     * <li>{@link #LISTENER_COMPONENT}</li>
     * <li>{@link #REGISTRATION_METHOD}</li>
     * <li>{@link #UNREGISTRATION_METHOD}</li>
     * </ul>
     */
    CompositeType   REGISTRATION_LISTENER_TYPE  = Item.compositeType(
                                                    REGISTRATION_LISTENER, 
                                                    "This type encapsulates RegistrationListener objects",
                                                    LISTENER_COMPONENT_ITEM,
                                                    REGISTRATION_METHOD_ITEM,
                                                    UNREGISTRATION_METHOD_ITEM);
    
    
    ///////////////////////////////////////////////////////////////
    // Define <service>'s CompositeType
    ///////////////////////////////////////////////////////////////
    
    /**
     * The key INTERFACES, used in {@link #INTERFACES_ITEM}.
     */
    String          INTERFACES              = "Interfaces";
    
    /**
     * The item containing the type names of the interfaces that the service should be advertised as supporting.
     * The key is {@link #INTERFACES}, and the type is {@link JmxConstants#STRING_ARRAY_TYPE}.
     */
    Item            INTERFACES_ITEM         = new Item(
                                                    INTERFACES, 
                                                    "The type names of the interfaces", 
                                                    JmxConstants.STRING_ARRAY_TYPE);
    /**
     * The key AUTO_EXPORT, used in {@link #AUTO_EXPORT_ITEM}.
     */
    String          AUTO_EXPORT             = "AutoExport";
    
    /**
     * The item containing the auto-export mode for the service.
     * The key is {@link #AUTO_EXPORT}, and the type is {@link SimpleType#INTEGER}.
     */
    //TODO describe integer
    Item            AUTO_EXPORT_ITEM        = new Item(
                                                    AUTO_EXPORT, 
                                                    "The auto-export mode for the service", 
                                                    SimpleType.INTEGER);

    /**
     * The key RANKING, used in {@link #RANKING_ITEM}.
     */
    String          RANKING                 = "Ranking";
    
    /**
     * The item containing the ranking value to use when advertising the service.
     * The key is {@link #RANKING}, and the type is {@link SimpleType#INTEGER}.
     */
    Item            RANKING_ITEM            = new Item(
                                                    RANKING, 
                                                    "The ranking value when advertising the service", 
                                                    SimpleType.INTEGER);

    /**
     * The key SERVICE_COMPONENT, used in {@link #SERVICE_COMPONENT_ITEM}.
     */
    String          SERVICE_COMPONENT       = "ServiceComponent";
    
    /**
     * The item containing the id of the component to be exported as a service.
     * The key is {@link #SERVICE_COMPONENT}, and the type is {@link JmxConstants#BYTE_ARRAY_TYPE}.
     */
    Item            SERVICE_COMPONENT_ITEM  = new Item(
                                                    SERVICE_COMPONENT, 
                                                    "The component to be exported as a service", 
                                                    JmxConstants.BYTE_ARRAY_TYPE);
    
    /**
     * The key SERVICE_PROPERTIES, used in {@link #SERVICE_PROPERTIES_ITEM}.
     */
    String          SERVICE_PROPERTIES      = "ServiceProperties";
    
    /**
     * The item containing the user declared properties to be advertised with the service.
     * The key is {@link #SERVICE_COMPONENT}, and the type is {@link SimpleType#STRING}.
     */
    Item            SERVICE_PROPERTIES_ITEM = new Item(
                                                    SERVICE_PROPERTIES,
                                                    "The user declared properties to be advertised with the service",
                                                    Item.arrayType(1, MAP_ENTRY_TYPE));
    
    /**
     * The key REGISTRATION_LISTENERS, used in {@link #REGISTRATION_LISTENERS_ITEM}.
     */
    String          REGISTRATION_LISTENERS  = "RegistrationListeners";
    
    /**
     * The item containing all the registration listeners.
     * The key is {@link #REGISTRATION_LISTENERS}, and the type is {@link ArrayType}.
     */
    Item            REGISTRATION_LISTENERS_ITEM = new Item(
                                                    REGISTRATION_LISTENERS,
                                                    "All the registration listeners",
                                                    Item.arrayType(1, REGISTRATION_LISTENER_TYPE));
    
    /**
     * The name of CompositeType for ServiceMetadata objects, used in {@link #SERVICE_METADATA_TYPE}.
     */
    String          SERVICE_METADATA               = "ServiceMetadata";    
    
    /**
     * The CompositeType for a ServiceMetadata object, it extends {@link #COMPONENT_METADATA_TYPE}
     * and adds the following items:
     * <ul>
     * <li>{@link #INTERFACES}</li>
     * <li>{@link #AUTO_EXPORT}</li>
     * <li>{@link #RANKING}</li>
     * <li>{@link #SERVICE_COMPONENT}</li>
     * <li>{@link #SERVICE_PROPERTIES}</li>
     * <li>{@link #REGISTRATION_LISTENERS}</li>
     * </ul>
     */
    CompositeType   SERVICE_METADATA_TYPE   = Item.extend(
                                                    COMPONENT_METADATA_TYPE, 
                                                    SERVICE_METADATA, 
                                                    "This type encapsulates ServiceMetadata objects",
                                                    INTERFACES_ITEM,
                                                    AUTO_EXPORT_ITEM,
                                                    RANKING_ITEM,
                                                    SERVICE_COMPONENT_ITEM,
                                                    SERVICE_PROPERTIES_ITEM,
                                                    REGISTRATION_LISTENERS_ITEM);

    ///////////////////////////////////////////////////////////////
    // Define <reference-listener>'s CompositeType
    // LISTENER_COMPONENT_ITEM defined in the <registration-listener>
    ///////////////////////////////////////////////////////////////
    
    /**
     * The key BIND_METHOD, used in {@link #BIND_METHOD_ITEM}.
     */
    String          BIND_METHOD                 = "BindMethod";
    
    /**
     * The item containing the name of the bind method.
     * The key is {@link #BIND_METHOD}, and the type is {@link SimpleType#STRING}.
     */
    Item            BIND_METHOD_ITEM            = new Item(
                                                    BIND_METHOD, 
                                                    "The name of the bind method", 
                                                    SimpleType.STRING);
    
    /**
     * The key UNBIND_METHOD, used in {@link #UNBIND_METHOD_ITEM}.
     */
    String          UNBIND_METHOD               = "UnbindMethod";
    
    /**
     * The item containing the name of the unbind method.
     * The key is {@link #UNBIND_METHOD}, and the type is {@link SimpleType#STRING}.
     */
    Item            UNBIND_METHOD_ITEM          = new Item(
                                                    UNBIND_METHOD, 
                                                    "The name of the unbind method", 
                                                    SimpleType.STRING);
    
    /**
     * The name of CompositeType for ReferenceListener objects, used in {@link #REFERENCE_LISTENER_TYPE}.
     */
    String          REFERENCE_LISTENER               = "ReferenceListener"; 
    
    /**
     * The CompositeType for a reference listener, and it contains the following items:
     * <ul>
     * <li>{@link #LISTENER_COMPONENT}</li>
     * <li>{@link #BIND_METHOD}</li>
     * <li>{@link #UNBIND_METHOD}</li>
     * </ul>
     */
    CompositeType   REFERENCE_LISTENER_TYPE  = Item.compositeType(
                                                    REFERENCE_LISTENER, 
                                                    "This type encapsulates ReferenceListener objects",
                                                    LISTENER_COMPONENT_ITEM,
                                                    BIND_METHOD_ITEM,
                                                    UNBIND_METHOD_ITEM);
    
    
    ///////////////////////////////////////////////////////////////
    // Define Service Reference's CompositeType, 
    // <reference> & <reference-list> will extend this
    ///////////////////////////////////////////////////////////////
    
    /**
     * The key AVAILABILITY, used in {@link #AVAILABILITY_ITEM}.
     */
    String          AVAILABILITY            = "Availability";
    
    /**
     * The item specify whether or not a matching service is required at all times.
     * The key is {@link #AVAILABILITY}, and the type is {@link SimpleType#INTEGER}.
     * 
     */
    //TODO add description for each int
    Item            AVAILABILITY_ITEM       = new Item(
                                                    AVAILABILITY, 
                                                    "To specify whether or not a matching service is required at all times", 
                                                    SimpleType.INTEGER);
    
    /**
     * The key INTERFACE, used in {@link #INTERFACE_ITEM}.
     */
    String          INTERFACE               = "Interface";
    
    /**
     * The item containing the name of the interface type that a matching service must support.
     * The key is {@link #INTERFACE}, and the type is {@link SimpleType#STRING}.
     */
    Item            INTERFACE_ITEM          = new Item(
                                                    INTERFACE, 
                                                    "the name of the interface type", 
                                                    SimpleType.STRING);
    
    /**
     * The key COMPONENT_NAME, used in {@link #COMPONENT_NAME_ITEM}.
     */
    String          COMPONENT_NAME          = "ComponentName";
    
    /**
     * The item containing the value of the <code>component-name</code> attribute of the service reference.
     * The key is {@link #INTERFACE}, and the type is {@link SimpleType#STRING}.
     */
    Item            COMPONENT_NAME_ITEM     = new Item(
                                                    COMPONENT_NAME, 
                                                    "The value of the component-name attribute of the service reference", 
                                                    SimpleType.STRING);
    /**
     * The key FILTER, used in {@link #FILTER_ITEM}.
     */
    String          FILTER                  = "Filter";
    
    /**
     * The item containing the filter expression that a matching service must match.
     * The key is {@link #FILTER}, and the type is {@link SimpleType#STRING}.
     */
    Item            FILTER_ITEM             = new Item(
                                                    FILTER, 
                                                    "The filter expression that a matching service must match", 
                                                    SimpleType.STRING);
    
    /**
     * The key REFERENCE_LISTENERS, used in {@link #REFERENCE_LISTENERS_ITEM}.
     */
    String          REFERENCE_LISTENERS     = "RegistrationListeners";
    
    /**
     * The item containing all the reference listeners.
     * The key is {@link #REFERENCE_LISTENERS}, and the type is {@link ArrayType}.
     */
    Item            REFERENCE_LISTENERS_ITEM= new Item(
                                                    REFERENCE_LISTENERS,
                                                    "All the reference listeners",
                                                    Item.arrayType(1, REFERENCE_LISTENER_TYPE));
    
    /**
     * The name of CompositeType for ServiceReferenceMetadata objects, used in {@link #SERVICE_REFERENCE_METADATA_TYPE}.
     */
    String          SERVICE_REFERENCE_METADATA               = "ServiceReferenceMetadata"; 
    
    /**
     * The CompositeType for a ServiceReferenceMetadata object, it extends 
     * {@link #COMPONENT_METADATA_TYPE} and adds the following items:
     * <ul>
     * <li>{@link #AVAILABILITY}</li>
     * <li>{@link #INTERFACE}</li>
     * <li>{@link #COMPONENT_NAME}</li>
     * <li>{@link #FILTER}</li>
     * <li>{@link #REFERENCE_LISTENERS}</li>
     * </ul>
     */
    CompositeType   SERVICE_REFERENCE_METADATA_TYPE  = Item.extend(
                                                    COMPONENT_METADATA_TYPE, 
                                                    SERVICE_REFERENCE_METADATA, 
                                                    "This type encapsulates ServiceReferenceMetadata objects",
                                                    AVAILABILITY_ITEM,
                                                    INTERFACE_ITEM,
                                                    COMPONENT_NAME_ITEM,
                                                    FILTER_ITEM,
                                                    REFERENCE_LISTENERS_ITEM);
    
    ///////////////////////////////////////////////////////////////
    // Define <reference>'s CompositeType
    ///////////////////////////////////////////////////////////////
    
    /**
     * The key TIME_OUT, used in {@link #TIMEOUT_ITEM}.
     */
    String          TIMEOUT                = "TimeOut";
    
    /**
     * The item containing the timeout for service invocations when a backing service is is unavailable.
     * The key is {@link #TIMEOUT}, and the type is {@link SimpleType#LONG}.
     */
    Item            TIMEOUT_ITEM           = new Item(
                                                    TIMEOUT, 
                                                    "The timeout for service invocations when a backing service is is unavailable", 
                                                    SimpleType.LONG);
    
    /**
     * The name of CompositeType for ReferenceMetadata objects, used in {@link #REFERENCE_METADATA_TYPE}.
     */
    String          REFERENCE_METADATA        = "ReferenceMetadata"; 
    
    /**
     * The CompositeType for a ReferenceMetadata object, it extends 
     * {@link #SERVICE_REFERENCE_METADATA_TYPE} and adds the following items:
     * <ul>
     * <li>{@link #TIMEOUT}</li>
     * </ul>
     */
    CompositeType   REFERENCE_METADATA_TYPE = Item.extend(
                                                    SERVICE_REFERENCE_METADATA_TYPE, 
                                                    REFERENCE_METADATA, 
                                                    "This type encapsulates ReferenceMetadata objects",
                                                    TIMEOUT_ITEM);
    
    ///////////////////////////////////////////////////////////////
    // Define <reference-list>'s CompositeType
    ///////////////////////////////////////////////////////////////
    
    /**
     * The key MEMBER_TYPE, used in {@link #MEMBER_TYPE_ITEM}.
     */
    String          MEMBER_TYPE             = "MemberType";
    
    /**
     * The item specify whether the List will contain service object proxies or ServiceReference objects.
     * The key is {@link #MEMBER_TYPE}, and the type is {@link SimpleType#INTEGER}.
     */
    Item            MEMBER_TYPE_ITEM        = new Item(
                                                    MEMBER_TYPE, 
                                                    "To specify whether the List will contain service object proxies or ServiceReference objects", 
                                                    SimpleType.INTEGER);
    
    /**
     * The name of CompositeType for ReferenceListMetadata objects, used in {@link #REFERENCE_LIST_METADATA_TYPE}.
     */
    String          REFERENCE_LIST_METADATA        = "ReferenceListMetadata"; 
    
    /**
     * The CompositeType for a ReferenceListMetadata object, it extends 
     * {@link #SERVICE_REFERENCE_METADATA_TYPE} and adds the following items:
     * <ul>
     * <li>{@link #MEMBER_TYPE}</li>
     * </ul>
     */
    CompositeType   REFERENCE_LIST_METADATA_TYPE = Item.extend(
                                                    SERVICE_REFERENCE_METADATA_TYPE, 
                                                    REFERENCE_LIST_METADATA, 
                                                    "This type encapsulates ReferenceListMetadata objects",
                                                    MEMBER_TYPE_ITEM);


    /**
     * Returns the list of component id.
     * 
     * @param containerServiceId The blueprint container service id
     * @return the array of component id
     */
    String[] getComponentIds(long containerServiceId);
    
    /**
     * Returns all component ids of the specified component type
     * 
     * @param containerServiceId The blueprint container service id
     * @param type The string used to specify the type of component
     * @return the array of component id
     */
    String[] getComponentIdsByType(long containerServiceId, String type);
    
    /**
     * Returns the ComponentMetadata according to the its component id.
     * The returned Composite Data's type is actually one of {@link #BEAN_METADATA_TYPE}, 
     * {@link #SERVICE_METADATA_TYPE}, {@link #REFERENCE_METADATA_TYPE}, REFERENCE_LIST_METADATA_TYPE.
     * 
     * @param containerServiceId The blueprint container service id
     * @param componentId The component id
     * @return the ComponentMetadata
     */
    CompositeData getComponentMetadata(long containerServiceId, String componentId);
        
    /**
     * Returns all the blueprint containers' service IDs, which successfully
     * created from blueprint bundles.
     * 
     * @return the list of all the service IDs of the blueprint containers created by current extender 
     * @throws IOException if the operation fails
     */
    long[] getBlueprintContainerServiceIds() throws IOException;
    
    /**
     * Returns the blueprint container's service id if the bundle specified 
     * by the bundle id is a blueprint bundle.
     * 
     * @param bundleId The bundle id of a blueprint bundle
     * @return the blueprint container's service id, or null if the blueprint bundle initialed failed.
     * @throws IOException if the operation fails
     * @throws IllegalArgumentException if the bundle is not a blueprint bundle
     */
    long getBlueprintContainerServiceId(long bundleId) throws IOException;
}
