
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

package org.apache.aries.samples.blog.persistence.jpa;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.apache.aries.blueprint.annotation.bean.Activation;
import org.apache.aries.blueprint.annotation.bean.Bean;
import org.apache.aries.blueprint.annotation.service.Service;
import org.apache.aries.samples.blog.api.persistence.BlogPersistenceService;
import org.apache.aries.samples.blog.api.persistence.Entry;
import org.apache.aries.samples.blog.persistence.jpa.entity.AuthorImpl;
import org.apache.aries.samples.blog.persistence.jpa.entity.EntryImpl;

/**
 * This class is the implementation of the blogPersistenceService
 */
@Service(classes = BlogPersistenceService.class)
@Transactional(Transactional.TxType.REQUIRED)
@Bean(activation = Activation.LAZY)
public class BlogPersistenceServiceImpl implements BlogPersistenceService {

    @PersistenceContext(unitName = "blogExample")
	EntityManager entityManager;

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void createAuthor(String email, Date dob, String name,
                             String displayName, String bio) {
		AuthorImpl a = new AuthorImpl();
		a.setEmail(email);
		a.setName(name);
		a.setDisplayName(displayName);
		a.setBio(bio);
		a.setDob(dob);
		entityManager.persist(a);
		
	}

	public void createBlogPost(String authorEmail, String title,
			String blogText, List<String> tags) {
	
		AuthorImpl a = entityManager.find(AuthorImpl.class, authorEmail);
		EntryImpl b = new EntryImpl();

		Date publishDate = new Date(System.currentTimeMillis());

		b.setBlogText(blogText);
		b.setAuthor(a);
		b.setTitle((title == null) ? "" : title);
		b.setPublishDate(publishDate);
		b.setTags((tags == null) ? new ArrayList<String>() : tags);

		a.updateEntries(b);
		entityManager.persist(b);
		entityManager.merge(b.getAuthor());
	}

	public Entry findBlogEntryByTitle(String title) {
		Query q = entityManager
				.createQuery("SELECT e FROM BLOGENTRY e WHERE e.title = ?1");
		q.setParameter(1, title);
		Entry b = (Entry) q.getSingleResult();
		return b;
	}

	public List<AuthorImpl> getAllAuthors() {
		@SuppressWarnings("unchecked")
		List<AuthorImpl> list = entityManager.createQuery("SELECT a FROM AUTHOR a")
				.getResultList();

		return list;
	}

	public List<EntryImpl> getAllBlogEntries() {
		@SuppressWarnings("unchecked")
		List<EntryImpl> list = entityManager.createQuery(
				"SELECT b FROM BLOGENTRY b ORDER BY b.publishDate DESC")
				.getResultList();
		return list;
		
	}

	public int getNoOfBlogEntries() {
		Number n = (Number) entityManager.createQuery(
				"SELECT COUNT(b) FROM BLOGENTRY b").getSingleResult();
		return n.intValue();
	}

	public List<EntryImpl> getBlogEntries(int firstPostIndex, int noOfPosts) {
		Query q = entityManager
				.createQuery("SELECT b FROM BLOGENTRY b ORDER BY b.publishDate DESC");
		q.setFirstResult(firstPostIndex);
		q.setMaxResults(noOfPosts);

		@SuppressWarnings("unchecked")
		List<EntryImpl> list = q.getResultList();

		return list;
	}

	public AuthorImpl getAuthor(String emailAddress) {
		AuthorImpl a = entityManager.find(AuthorImpl.class, emailAddress);
		return a;
	}

	public List<EntryImpl> getBlogEntriesModifiedBetween(Date start, Date end) {
		Query q = entityManager
				.createQuery("SELECT b FROM BLOGENTRY b WHERE (b.updatedDate >= :start AND b.updatedDate <= :end) OR (b.publishDate >= :start AND b.publishDate <= :end) ORDER BY b.publishDate ASC");
		q.setParameter("start", start);
		q.setParameter("end", end);

		@SuppressWarnings("unchecked")
		List<EntryImpl> list = q.getResultList();

		return list;
	}
	
	public List<EntryImpl> getBlogsForAuthor(String emailAddress) {

		List<EntryImpl> list = entityManager.find(AuthorImpl.class, emailAddress)
				.getEntries();
		
		return list;

	}

	public void updateAuthor(String email, Date dob, String name,
			String displayName, String bio) {
		AuthorImpl a = entityManager.find(AuthorImpl.class, email);
		a.setEmail(email);
		a.setName(name);
		a.setDisplayName(displayName);
		a.setBio(bio);
		a.setDob(dob);
		entityManager.merge(a);
	}
	
	public void updateBlogEntry(long id, String email, String title,
			String blogText, List<String> tags, Date updatedDate) {
		EntryImpl b = entityManager.find(EntryImpl.class, id);
		b.setTitle(title);
		b.setBlogText(blogText);
		b.setTags(tags);
		b.setUpdatedDate(updatedDate);

		entityManager.merge(b);
	}

	public void removeAuthor(String emailAddress) {
		entityManager.remove(entityManager.find(AuthorImpl.class, emailAddress));
	}

	public void removeBlogEntry(long id) {
		EntryImpl b = entityManager.find(EntryImpl.class, id);
		b = entityManager.merge(b);
		b.getAuthor().getEntries().remove(b);

		entityManager.remove(entityManager.merge(b));
		entityManager.merge(b.getAuthor());

	}

	public EntryImpl getBlogEntryById(long postId) {
		EntryImpl b =  entityManager.find(EntryImpl.class, postId);
		return b;
	}

	public void setPublishDate (long postId, Date date) {
		//Added for testing
		EntryImpl b = entityManager.find(EntryImpl.class, postId);
		b.setPublishDate(date);	
		entityManager.merge(b);
	}
	
	public void setUpdatedDate (long postId, Date date) {
		//Added for testing
		EntryImpl b = entityManager.find(EntryImpl.class, postId);
		b.setUpdatedDate(date);	
		entityManager.merge(b);
	}
}
