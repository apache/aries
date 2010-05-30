/*
 * Statements
 *
 * Copyright (c) 2010 Sabre Holdings, Inc. All Rights Reserved.
 */
package org.apache.aries.samples.blog.persistence.jdbc;

public class Statements {

    private static final String I_THR_TRY_ELEMENT = "I_THR_TRY_ELEMENT";

    private static final String I_THR_TRY_AUTHOR_EMAIL = "I_THR_TRY_AUTHOR_EMAIL";

    public static final String I_BLGNTRY_AUTHOR = "I_BLGNTRY_AUTHOR";

    public static final String AUTHOR_TABLE_NAME = "AUTHOR";

    public static final String AUTHOR_BLOG_ENTRY_TABLE_NAME = "AUTHOR_BLOGENTRY";

    public static final String BLOG_ENTRY_TABLE_NAME = "BLOGENTRY";

    public static final String COMMENT_ENTRY_TABLE_NAME = "COMMENT";

    private String[] dropSchemaStatements;

    private String[] createSchemaStatements;

    public synchronized String[] getCreateSchemaStatements() {
        if (createSchemaStatements == null) {
            createSchemaStatements = new String[] {
                    "CREATE TABLE "
                            + AUTHOR_TABLE_NAME
                            + " (email VARCHAR(255) NOT NULL, "
                            + "bio VARCHAR(255), displayName VARCHAR(255), "
                            + "dob TIMESTAMP, name VARCHAR(255), PRIMARY KEY (email))",
                    "CREATE TABLE " + AUTHOR_BLOG_ENTRY_TABLE_NAME
                            + " (AUTHOR_EMAIL VARCHAR(255), POSTS_ID BIGINT)",
                    "CREATE TABLE "
                            + BLOG_ENTRY_TABLE_NAME
                            + " (id BIGINT NOT NULL, blogText VARCHAR(10000), "
                            + "publishDate TIMESTAMP, title VARCHAR(255), updatedDate TIMESTAMP, "
                            + "AUTHOR_EMAIL VARCHAR(255), PRIMARY KEY (id))",
                    "CREATE TABLE "
                            + COMMENT_ENTRY_TABLE_NAME
                            + " (id BIGINT NOT NULL, comment VARCHAR(255), creationDate TIMESTAMP, "
                            + "AUTHOR_EMAIL VARCHAR(255), BLOGENTRY_ID BIGINT)",
                    "CREATE INDEX " + I_THR_TRY_AUTHOR_EMAIL + " ON "
                            + AUTHOR_BLOG_ENTRY_TABLE_NAME + " (AUTHOR_EMAIL)",
                    "CREATE INDEX " + I_THR_TRY_ELEMENT + " ON "
                            + AUTHOR_BLOG_ENTRY_TABLE_NAME + " (POSTS_ID)",
                    "CREATE INDEX " + I_BLGNTRY_AUTHOR + " ON "
                            + BLOG_ENTRY_TABLE_NAME + " (AUTHOR_EMAIL)",
                    "DELETE FROM " + AUTHOR_TABLE_NAME,
                    "DELETE FROM " + AUTHOR_BLOG_ENTRY_TABLE_NAME,
                    "DELETE FROM " + BLOG_ENTRY_TABLE_NAME,
                    "DELETE FROM " + COMMENT_ENTRY_TABLE_NAME };
        }
        return createSchemaStatements;
    }

    public synchronized String[] getDropSchemaStatements() {
        if (dropSchemaStatements == null) {
            dropSchemaStatements = new String[] {
                    "DROP INDEX " + I_THR_TRY_ELEMENT,
                    "DROP INDEX " + I_BLGNTRY_AUTHOR,
                    "DROP INDEX " + I_THR_TRY_AUTHOR_EMAIL,
                    "DROP TABLE " + COMMENT_ENTRY_TABLE_NAME,
                    "DROP TABLE " + BLOG_ENTRY_TABLE_NAME,
                    "DROP TABLE " + AUTHOR_BLOG_ENTRY_TABLE_NAME,
                    "DROP TABLE " + AUTHOR_TABLE_NAME };
        }
        return dropSchemaStatements;
    }

}
