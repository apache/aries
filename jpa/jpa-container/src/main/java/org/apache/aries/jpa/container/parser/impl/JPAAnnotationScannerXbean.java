/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.container.parser.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.apache.xbean.finder.Annotated;
import org.apache.xbean.finder.BundleAnnotationFinder;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
class JPAAnnotationScannerXbean {
    private static Logger LOGGER = LoggerFactory.getLogger(JPAAnnotationScannerXbean.class);

    public static Collection<String> findJPAAnnotatedClasses(Bundle bundle, PackageAdmin packageAdmin) {
        Collection<String> classes = new ArrayList<String>();
        try {
            BundleAnnotationFinder finder = new BundleAnnotationFinder(packageAdmin, bundle);
            Set<Annotated<Class<?>>> annotatedSet = new HashSet<Annotated<Class<?>>>();
            annotatedSet.addAll(finder.findMetaAnnotatedClasses(Entity.class));
            annotatedSet.addAll(finder.findMetaAnnotatedClasses(MappedSuperclass.class));
            annotatedSet.addAll(finder.findMetaAnnotatedClasses(Embeddable.class));
            LOGGER.info("Searching for entities");
            for (Annotated<Class<?>> annotated : annotatedSet) {
                LOGGER.info("Found entity " + annotated.get().getName());
                classes.add(annotated.get().getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classes;
    }

}