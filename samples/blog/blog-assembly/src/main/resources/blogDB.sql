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
connect 'jdbc:derby:blogDB/;create=true';
CREATE TABLE AUTHOR (email VARCHAR(255) NOT NULL, bio VARCHAR(255), displayName VARCHAR(255), dob TIMESTAMP, name VARCHAR(255), PRIMARY KEY (email));
CREATE TABLE AUTHOR_BLOGENTRY (AUTHOR_EMAIL VARCHAR(255), POSTS_ID BIGINT);
CREATE TABLE BLOGENTRY (id BIGINT NOT NULL, blogText VARCHAR(10000), publishDate TIMESTAMP, title VARCHAR(255), updatedDate TIMESTAMP, AUTHOR_EMAIL VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE COMMENT (id BIGINT NOT NULL, comment VARCHAR(255), creationDate TIMESTAMP, AUTHOR_EMAIL VARCHAR(255), BLOGENTRY_ID BIGINT);
CREATE TABLE OPENJPA_SEQUENCE_TABLE (ID SMALLINT NOT NULL, SEQUENCE_VALUE BIGINT, PRIMARY KEY (ID));
CREATE INDEX I_THR_TRY_AUTHOR_EMAIL ON AUTHOR_BLOGENTRY (AUTHOR_EMAIL);
CREATE INDEX I_THR_TRY_ELEMENT ON AUTHOR_BLOGENTRY (POSTS_ID);
CREATE INDEX I_BLGNTRY_AUTHOR ON BLOGENTRY (AUTHOR_EMAIL);
DELETE FROM AUTHOR;
DELETE FROM AUTHOR_BLOGENTRY;
DELETE FROM BLOGENTRY;
DELETE FROM COMMENT;
DELETE FROM OPENJPA_SEQUENCE_TABLE;
exit;
