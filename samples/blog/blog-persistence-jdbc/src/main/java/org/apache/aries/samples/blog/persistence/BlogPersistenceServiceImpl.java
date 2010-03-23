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
package org.apache.aries.samples.blog.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.aries.samples.blog.api.persistence.BlogPersistenceService;
import org.apache.aries.samples.blog.persistence.entity.AuthorImpl;
import org.apache.aries.samples.blog.persistence.entity.EntryImpl;

/**
 * This class is the implementation of the blogPersistenceService
 */
public class BlogPersistenceServiceImpl implements BlogPersistenceService {
	private DataSource dataSource;
	

	/**
	 * set data source
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Create an author record
	 * 
	 * @param a
	 *            The author object to be created
	 * @throws ParseException
	 * @throws ParseException
	 */
	public void createAuthor(String email, Date dob, String name,
			String displayName, String bio) {
		
		
		try {
			Connection connection = dataSource.getConnection();
			String sql = "INSERT INTO AUTHOR VALUES (?,?,?,?,?)";
			
			PreparedStatement ppsm = connection.prepareStatement(sql);
			ppsm.setString(1, email);
			ppsm.setString(2, bio);
			ppsm.setString(3, displayName);
			if (dob != null)
				ppsm.setDate(4, new java.sql.Date(dob.getTime()));
			else
				ppsm.setDate(4, null);
			ppsm.setString(5, name);
			int insertRows = ppsm.executeUpdate();
			ppsm.close();
			connection.close();
			
			if (insertRows != 1)
				throw new IllegalArgumentException("The Author " + email
						+ " cannot be inserted.");
		} catch (SQLException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e.getMessage());
		}

	}

	/**
	 * Create a blog entry record
	 * 
	 * @param a 
	 * 			The author
	 * @param title 
	 * 			The title of the post
	 * @param blogText 
	 * 			The text of the post
	 * @param tags
	 * 
	 */
	public void createBlogPost(String authorEmail, String title, String blogText,
			List<String> tags) {
		
		AuthorImpl a = getAuthor(authorEmail);
		
		if(title == null) title = "";
		Date publishDate = new Date(System.currentTimeMillis());
		if(tags == null) tags = new ArrayList<String>();
		

		try {
			Connection connection = dataSource.getConnection();
			// let's find the max id from the blogentry table
			String sql = "SELECT max(id) FROM BLOGENTRY";
			PreparedStatement ppsm = connection.prepareStatement(sql);
			ResultSet rs = ppsm.executeQuery();
			// we only expect to have one row returned
			rs.next();

			long max_id = rs.getLong(1);
			ppsm.close();
			
			long post_id = max_id + 1;
			sql = "INSERT INTO BLOGENTRY VALUES (?,?,?,?,?,?)";
			
		    ppsm = connection.prepareStatement(sql);
			ppsm.setLong(1, post_id);
			ppsm.setString(2, blogText);
			if (publishDate != null)
				ppsm
						.setDate(3, new java.sql.Date(publishDate
								.getTime()));
			else
				ppsm.setDate(3, null);
			ppsm.setString(4, title);
			
		    ppsm.setDate(5, null);
			ppsm.setString(6, a.getEmail());
			int rows = ppsm.executeUpdate();
			if (rows != 1)
				throw new IllegalArgumentException(
						"The blog entry record cannot be inserted: "
								+ blogText);
			ppsm.close();
			
			// insert a row in the relationship table

			sql = "INSERT INTO Author_BlogEntry VALUES (?,?)";
			ppsm = connection.prepareStatement(sql);
			ppsm.setString(1, a.getEmail());
			ppsm.setLong(2, post_id);

			rows = ppsm.executeUpdate();
			ppsm.close();
			connection.close();
			
			if (rows != 1)
				throw new IllegalArgumentException(
						"The Author_BlogEntry record cannot be inserted: "
								+ a.getEmail() + " , " + post_id);

		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}

	}

	/**
	 * Find the blog entry record with the specified title
	 * 
	 * @param The title to be searched
	 * @return The blogEntry record
	 */
	public EntryImpl findBlogEntryByTitle(String title) {

		EntryImpl be = null;

		String sql = "SELECT * FROM BlogEntry e WHERE e.title = '" + title
				+ "'";

		List<EntryImpl> blogEntries = findBlogs(sql);
		
		// just return the first blog entry for the time being
		if ((blogEntries != null) && (blogEntries.size() > 0))
			be = blogEntries.get(0);
		return be;
	}

	/**
	 * Return all author records in the Author table
	 * 
	 * @return the list of Author records
	 */
	public List<AuthorImpl> getAllAuthors() {
		String sql = "SELECT * FROM Author";

		List<AuthorImpl> list = findAuthors(sql);

		return list;
	}

	/**
	 * Return all blog entry records from BlogEntry table with the most recent
	 * published blog entries first
	 * 
	 * @return a list of blogEntry object
	 */
	public List<EntryImpl> getAllBlogEntries() {
		String sql = "SELECT * FROM BlogEntry b ORDER BY b.publishDate DESC";

		List<EntryImpl> list = findBlogs(sql);

		return list;
	}

	/**
	 * Return the number of the blog entry records
	 * 
	 * @return the number of the blog Entry records
	 */
	public int getNoOfBlogEntries() {

		int count = 0;

		String sql = "SELECT count(*) FROM BLOGENTRY";
		try {
			Connection connection = dataSource.getConnection();
			PreparedStatement ppsm = connection.prepareStatement(sql);
			ResultSet rs = ppsm.executeQuery();
			// we only expect to have one row returned
			rs.next();
			count = rs.getInt(1);
			ppsm.close();
			connection.close();

		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		return count;
	}

	/**
	 * Return the portion of blog Entries
	 * 
	 * @param firstPostIndex
	 *            The index of the first blog entry to be returned
	 * @param noOfPosts
	 *            The number of blog entry to be returned
	 * @return The list of the blog entry record
	 */
	public List<EntryImpl> getBlogEntries(int firstPostIndex, int noOfPosts) {
		String sql = "SELECT * FROM BlogEntry b ORDER BY b.publishDate DESC";
		List<EntryImpl> emptyList = new ArrayList<EntryImpl>();
		List<EntryImpl> blogs = findBlogs(sql);
		// we only return a portion of the list
		if (blogs == null)
			return emptyList;
		// We need to make sure we won't throw IndexOutOfBoundException if the
		// supplied
		// index is out of the list range
		int maximumIndex = blogs.size();
		// if the first index is minus or greater than the last item index of
		// the list, return an empty list
		if ((firstPostIndex < 0) || (noOfPosts <= 0)
				|| (firstPostIndex > maximumIndex))
			return emptyList;
		// return the required number of the blog entries or the available blog
		// entries
		int lastIndex = noOfPosts + firstPostIndex;
		// we need to make sure we return the blog entries at most up to the
		// final record

		return (blogs.subList(firstPostIndex,
				(lastIndex > maximumIndex) ? maximumIndex : lastIndex));

	}

	/**
	 * Return the author with the specified email address
	 * 
	 * @param emailAddress
	 *            The email address
	 * @return The author record
	 */
	public AuthorImpl getAuthor(String emailAddress) {
		String sql = "SELECT * FROM AUTHOR a where a.email='" + emailAddress
				+ "'";
		List<AuthorImpl> authors = findAuthors(sql);

		if (authors.size() == 0)
			return null;
		else if (authors.size() > 1)
			throw new IllegalArgumentException(
					"Email address should be unique per author");

		return authors.get(0); // just return the first author
	}

	/**
	 * Return the blog entries modified between the date range of [start, end]
	 * 
	 * @param start
	 *            The start date
	 * @param end
	 *            The end date
	 * @return The list of blog entries
	 */
	public List<EntryImpl> getBlogEntriesModifiedBetween(Date start, Date end) {

		// String sql = "SELECT * FROM BlogEntry b WHERE (b.updatedDate >= " +
		// startTS +" AND b.updatedDate <= " + endTS + ") OR (b.publishDate >= "
		// +startTS + " AND b.publishDate <= " + endTS +
		// ") ORDER BY b.publishDate ASC";
		String sql = "SELECT * FROM BlogEntry b WHERE (Date(b.updatedDate) BETWEEN '"
				+ start
				+ "' AND '"
				+ end
				+ "') OR (Date(b.publishDate) BETWEEN '"
				+ start
				+ "' AND  '"
				+ end + "') ORDER BY b.publishDate ASC";

		return findBlogs(sql);

	}

	/**
	 * Return a list of blog entries belonging to the author with the specified
	 * email address
	 * 
	 * @param emailAddress
	 *            the author's email address
	 * @return The list of blog entries
	 */
	public List<EntryImpl> getBlogsForAuthor(String emailAddress) {

		String sql = "SELECT * FROM BlogEntry b WHERE b.AUTHOR_EMAIL='"
				+ emailAddress + "'";
		return findBlogs(sql);
	}

	/**
	 * Update the author record
	 * 
	 * @param email
	 * 			The email associated with an author
	 * @param dob
	 * 			The author's date of birth
	 * @param name
	 * 			the author's name
	 * @param displayName
	 * 			The displayName
	 * @param bio
	 * 			The aouthor's bio
	 */
	public void updateAuthor(String email, Date dob, String name,
			String displayName, String bio) {

		
		String sql = "UPDATE AUTHOR a SET bio = ?, displayName = ?, dob = ?, name =? WHERE email ='"
				+ email + "'";
		int updatedRows = 0;
		try {
			Connection connection = dataSource.getConnection();
			PreparedStatement ppsm = connection.prepareStatement(sql);
			ppsm.setString(1, bio);
			ppsm.setString(2, displayName);
			if (dob != null)
				ppsm.setDate(3, new java.sql.Date(dob.getTime()));
			else
				ppsm.setDate(3, null);
			ppsm.setString(4, name);
			updatedRows = ppsm.executeUpdate();
			
			ppsm.close();
			connection.close();
			
			if (updatedRows != 1)
				throw new IllegalArgumentException("The Author " + email
						+ " cannot be updated.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Update the blog entry record
	 * 
	 * 
	 */
	public void updateBlogEntry(long id, String email, String title, String blogText, List<String> tags, Date updatedDate) {
		
		if (id == -1)
			throw new IllegalArgumentException(
					"Not a BlogEntry returned by this interface");
		EntryImpl b = getBlogEntryById(id);
		String sql_se = "SELECT * FROM BLOGENTRY bp WHERE bp.id = " + id;
		String email_old = null;
		// let's find out the email address for the blog post to see whether the
		// table Author_BlogEntry needs to be updated
		// if the email is updated, we need to update the table Author_BlogEntry
		// to reflect the change.
		try {
			Connection connection = dataSource.getConnection();
			PreparedStatement ppsm = connection.prepareStatement(sql_se);
			ResultSet rs = ppsm.executeQuery();
			// there should be just one record
			rs.next();
			email_old = rs.getString("AUTHOR_EMAIL");
			ppsm.close();
			connection.close();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		String sql = "UPDATE BLOGENTRY bp SET bp.blogText = ?, bp.publishDate = ?, bp.title = ?, bp.updatedDate = ?, bp.AUTHOR_EMAIL = ? where bp.id = "
				+ id;
		int updatedRows = 0;
		
		try {
			Connection connection = dataSource.getConnection();
			PreparedStatement ppsm = connection.prepareStatement(sql);
			ppsm.setString(1, blogText);
			if (b.getPublishDate() != null)
				ppsm
						.setDate(2, new java.sql.Date(b.getPublishDate()
								.getTime()));
			else
				ppsm.setDate(2, null);
			ppsm.setString(3, b.getTitle());
			if (b.getUpdatedDate() != null)
				ppsm
						.setDate(4, new java.sql.Date(b.getUpdatedDate()
								.getTime()));
			else
				ppsm.setDate(4, null);

			ppsm.setString(5, email);
			updatedRows = ppsm.executeUpdate();
			
			ppsm.close();
			
			connection.close();

			if (updatedRows != 1)
				throw new IllegalArgumentException("The Blog " + b.getId()
						+ " cannot be updated.");
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// if the email address is changed, we need to need to update the
		// relationship table Author_BlogEntry
		if ((email_old != null) && (!!!email_old.equals(email))) {
			// update the table Author_BlogEntry
			String sql_ab = "UDPATE Author_BlogEntry ab SET ab.AUTHOR_EMAIL = '"
					+ email + "'";
			updatedRows = 0;
			try {
				Connection connection = dataSource.getConnection();
				PreparedStatement ppsm = connection.prepareStatement(sql_ab);
				updatedRows = ppsm.executeUpdate();
				ppsm.close();
                connection.close();
				if (updatedRows != 1)
					throw new IllegalArgumentException(
							"The Author_BlogEntry with the postsID "
									+ b.getId() + " cannot be updated.");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Delete the author record with the specified email address
	 * 
	 * @param emailAddress
	 *            The author's email address
	 * 
	 */
	public void removeAuthor(String emailAddress) {

		// we need to remove the author and its blog entries

		try {
			String sql = "DELETE FROM BLOGENTRY bp WHERE bp.AUTHOR_EMAIL = '"
					+ emailAddress + "'";
			Connection connection = dataSource.getConnection();
			PreparedStatement ppsm = connection.prepareStatement(sql);
			ppsm.executeUpdate();
			ppsm.close();
			
			// delete the records from Author_BlogEntry
			sql = "DELETE FROM Author_BlogEntry ab WHERE ab.AUTHOR_EMAIL = '"
					+ emailAddress + "'";
			ppsm = connection.prepareStatement(sql);
			ppsm.executeUpdate();
			ppsm.close();

			// delete the author record
			sql = "DELETE FROM Author a WHERE a.EMAIL = '" + emailAddress + "'";
			ppsm = connection.prepareStatement(sql);
			ppsm.executeUpdate();
			ppsm.close();
			connection.close();

		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}

	}

	/**
	 * Delete the blog entry record specified by the blogEntry
	 * 
	 * @param blogEntry
	 *            the blog entry record to be deleted
	 */
	public void removeBlogEntry(long id) {
		if (id == -1)
			throw new IllegalArgumentException(
					"Not a BlogEntry returned by this interface");

		try {
			String sql = "DELETE FROM BLOGENTRY bp WHERE bp.id = "
					+ id;
			Connection connection = dataSource.getConnection();
			PreparedStatement ppsm = connection.prepareStatement(sql);
			ppsm.executeUpdate();
			ppsm.close();
			// We also need to delete the records from Author_BlogEntry, as this
			// table is a kind of link between author and blogentry record
			sql = "DELETE FROM Author_BlogEntry ab WHERE ab.POSTS_ID = "
					+ id;
			ppsm = connection.prepareStatement(sql);
			ppsm.executeUpdate();
			ppsm.close();
			connection.close();

		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}

	}

	/**
	 * Return the blog entry record with the specified id
	 * 
	 * @param postId
	 *            The blogEntry record id
	 */
	public EntryImpl getBlogEntryById(long postId) {
		String sql = "SELECT * FROM BlogEntry b WHERE b.id = " + postId;
		List<EntryImpl> blogs = findBlogs(sql);
		if (blogs.size() == 0)
			return null;
		if (blogs.size() > 1)
			throw new IllegalArgumentException("Blog id is not unique");
		return blogs.get(0);
	}

	/**
	 * Return a list of authors with the sql query
	 * 
	 * @param sql
	 *            The SQL query
	 * @return A list of author records
	 */
	private List<AuthorImpl> findAuthors(String sql) {
		List<AuthorImpl> authorList = new ArrayList<AuthorImpl>();

		try {
			Connection connection = dataSource.getConnection();
			PreparedStatement ppsm = connection.prepareStatement(sql);

			ResultSet ars = ppsm.executeQuery();

			while (ars.next()) {
				AuthorImpl ar = new AuthorImpl();
				ar.setBio(ars.getString("bio"));
				ar.setDisplayName(ars.getString("displayName"));
				ar.setDob(ars.getDate("dob"));
				String email = ars.getString("email");
				ar.setEmail(email);
				ar.setName(ars.getString("name"));

				// let's find the blog entries for the author
				String sql_be = "SELECT * FROM BLOGENTRY be WHERE be.AUTHOR_EMAIL = '"
						+ email + "'";
				PreparedStatement ppsm2 = connection.prepareStatement(sql_be);
				ResultSet rs = ppsm2.executeQuery();

				List<EntryImpl> blogs = new ArrayList<EntryImpl>();
				while (rs.next()) {
					EntryImpl blog = new EntryImpl();
					blog.setAuthor(ar);
					blog.setId(rs.getLong("id"));
					blog.setBlogText(rs.getString("blogText"));
					blog.setPublishDate(rs.getDate("publishDate"));
					blog.setTitle(rs.getString("title"));
					blog.setUpdatedDate(rs.getDate("updatedDate"));
					blogs.add(blog);
				}
				ar.setEntries(blogs);
				authorList.add(ar);
				ppsm2.close();
			}
		    ppsm.close();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return authorList;
	}

	/**
	 * Return a list of blog entries with the sql query
	 * 
	 * @param sql
	 *            The sql query to be executed
	 * @return a list of blogEntry records
	 */
	private List<EntryImpl> findBlogs(String sql) {
		List<EntryImpl> blogEntryList = new ArrayList<EntryImpl>();

		try {
			Connection connection = dataSource.getConnection();
			PreparedStatement ppsm = connection.prepareStatement(sql);
			ResultSet blogrs = ppsm.executeQuery();

			while (blogrs.next()) {
				EntryImpl be = new EntryImpl();
				be.setId(blogrs.getLong("id"));
				be.setBlogText(blogrs.getString("blogText"));
				be.setPublishDate(blogrs.getDate("publishDate"));
				be.setTitle(blogrs.getString("title"));
				be.setUpdatedDate(blogrs.getDate("updatedDate"));
				// find the author email address
				String author_email = blogrs.getString("AUTHOR_EMAIL");
				String author_sql = "SELECT * FROM Author a WHERE a.email ='"
						+ author_email + "'";
				List<AuthorImpl> authors = findAuthors(author_sql);
				// there should be just one entry, as email is the primary key
				// for the Author table
				if (authors.size() != 1)
					throw new IllegalArgumentException(
							"We got more than one author with the same email address. This is wrong");
				else
					be.setAuthor(authors.get(0));
				blogEntryList.add(be);
			}
			ppsm.close();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return blogEntryList;
	}
}
