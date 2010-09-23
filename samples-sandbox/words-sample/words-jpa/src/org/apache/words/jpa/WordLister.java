/**
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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.words.jpa;

import java.util.Random;

import org.apache.words.WordGetterService;

/**
 * 
 * A class which provides a random word. This implementation is deliberately
 * simple and cycles through only three words so that the same words are likely
 * to crop up more than once in testing.
 * 
 */
public class WordLister implements WordGetterService {
	/** A list of three words we'll cycle through at random. */
	String[] words = { "computers", "Java", "coffee" };

	public WordLister() {
	}

	@Override
	public String getRandomWord() {
		return words[new Random().nextInt(3)];
	}

}
