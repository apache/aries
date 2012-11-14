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
package org.apache.aries.jmx.blueprint.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.IdRefMetadata;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;

public class Util {

    public static BPMetadata metadata2BPMetadata(Metadata metadata) {
        if(null == metadata)
            return null;
        // target first
        if (metadata instanceof BeanMetadata)
            return new BPBeanMetadata((BeanMetadata) metadata);

        if (metadata instanceof ReferenceMetadata)
            return new BPReferenceMetadata((ReferenceMetadata) metadata);

        if (metadata instanceof RefMetadata)
            return new BPRefMetadata((RefMetadata) metadata);

        // others
        if (metadata instanceof CollectionMetadata)
            return new BPCollectionMetadata((CollectionMetadata) metadata);

        if (metadata instanceof ServiceMetadata)
            return new BPServiceMetadata((ServiceMetadata) metadata);

        if (metadata instanceof ReferenceListMetadata)
            return new BPReferenceListMetadata((ReferenceListMetadata) metadata);

        if (metadata instanceof IdRefMetadata)
            return new BPIdRefMetadata((IdRefMetadata) metadata);

        if (metadata instanceof MapMetadata)
            return new BPMapMetadata((MapMetadata) metadata);

        if (metadata instanceof PropsMetadata)
            return new BPPropsMetadata((PropsMetadata) metadata);

        if (metadata instanceof ValueMetadata)
            return new BPValueMetadata((ValueMetadata) metadata);

        // null last
        if (metadata instanceof NullMetadata)
            return new BPNullMetadata((NullMetadata) metadata);

        throw new RuntimeException("Unknown metadata type");
    }

    public static BPMetadata binary2BPMetadata(byte[] buf) {
        if(null == buf)
            return null;

        ByteArrayInputStream inBytes = new ByteArrayInputStream(buf);
        ObjectInputStream inObject;
        CompositeData metadata;
        try {
            inObject = new ObjectInputStream(inBytes);
            metadata = (CompositeData) inObject.readObject();
            inObject.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        String typename = metadata.getCompositeType().getTypeName();

        // target first
        if (typename.equals(BlueprintMetadataMBean.BEAN_METADATA))
            return new BPBeanMetadata(metadata);

        if (typename.equals(BlueprintMetadataMBean.REFERENCE_METADATA))
            return new BPReferenceMetadata(metadata);

        if (typename.equals(BlueprintMetadataMBean.REF_METADATA))
            return new BPRefMetadata(metadata);

        // others
        if (typename.equals(BlueprintMetadataMBean.COLLECTION_METADATA))
            return new BPCollectionMetadata(metadata);

        if (typename.equals(BlueprintMetadataMBean.SERVICE_METADATA))
            return new BPServiceMetadata(metadata);

        if (typename.equals(BlueprintMetadataMBean.REFERENCE_LIST_METADATA))
            return new BPReferenceListMetadata(metadata);

        if (typename.equals(BlueprintMetadataMBean.ID_REF_METADATA))
            return new BPIdRefMetadata(metadata);

        if (typename.equals(BlueprintMetadataMBean.MAP_METADATA))
            return new BPMapMetadata(metadata);

        if (typename.equals(BlueprintMetadataMBean.PROPS_METADATA))
            return new BPPropsMetadata(metadata);

        if (typename.equals(BlueprintMetadataMBean.VALUE_METADATA))
            return new BPValueMetadata(metadata);

        // null last
        if (metadata instanceof NullMetadata)
            return new BPNullMetadata(metadata);

        throw new RuntimeException("Unknown metadata type");
    }

    public static byte[] bpMetadata2Binary(BPMetadata metadata) {
        if(null == metadata)
            return null;

        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ObjectOutputStream outObject;
        try {
            outObject = new ObjectOutputStream(outBytes);
            outObject.writeObject(metadata.asCompositeData());
            outObject.close();
        } catch (IOException e) {// there is no io op
            throw new RuntimeException(e);
        }

        return outBytes.toByteArray();
    }
    public static Byte[] bpMetadata2BoxedBinary(BPMetadata metadata)
    {
        if(null == metadata)
            return null;

        byte [] src = bpMetadata2Binary(metadata);
        Byte [] res = new Byte[src.length];
        for(int i=0;i<src.length;i++)
        {
            res[i] = src[i];
        }
        return res;
    }
    public static BPMetadata boxedBinary2BPMetadata(Byte[] buf) {
        if(null == buf)
            return null;

        byte [] unbox = new byte[buf.length];
        for(int i=0;i<buf.length;i++)
        {
            unbox[i] = buf[i];
        }
        return binary2BPMetadata(unbox);
    }
}
