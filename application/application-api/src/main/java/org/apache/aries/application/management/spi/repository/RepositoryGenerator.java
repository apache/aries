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
package org.apache.aries.application.management.spi.repository;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.modelling.ModelledResource;

public interface RepositoryGenerator
{
  /**
   * Generate repository and store the content in the output stream.
   * @param repositoryName The repository name
   * @param byValueBundles By value bundles
   * @param os output stream
   * @throws ResolverException
   * @throws IOException
   */
  void generateRepository(String repositoryName, Collection<? extends ModelledResource> byValueBundles, OutputStream os) throws ResolverException, IOException;
  
}
