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
package org.apache.aries.samples.blog.comment.ejb;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.aries.samples.blog.api.comment.persistence.BlogCommentService;
import org.apache.aries.samples.blog.api.comment.persistence.Comment;

@Stateless(name="Commenting")
public class BlogCommentEJB implements BlogCommentService {

  @PersistenceContext(unitName="blogComments")
  private EntityManager commentEM;
  
  public void createComment(String comment, String author, long entryId) {
    
    commentEM.persist(new CommentImpl(comment, author, entryId));
  }

  public void delete(int id) {
    
    CommentImpl c = commentEM.find(CommentImpl.class, id);
    
    if(c != null)
      commentEM.remove(c);
  }

  
  public List<? extends Comment> getCommentsForAuthor(String authorId) {
    
    TypedQuery<CommentImpl> q = commentEM.createQuery(
        "SELECT c FROM Comment c WHERE c.authorId = :authorId", CommentImpl.class);
    
    q.setParameter("authorId", authorId);
    
    return q.getResultList();
  }

  public List<? extends Comment> getCommentsForEntry(long entryId) {

    TypedQuery<CommentImpl> q = commentEM.createQuery(
          "SELECT c FROM Comment c WHERE c.entryId = :entryId", CommentImpl.class);
    q.setParameter("entryId", entryId);
      
    return q.getResultList();
  }
}
