connect 'jdbc:derby:wordassociationDB/;create=true';
CREATE TABLE ASSOCIATION (word VARCHAR(255) NOT NULL, associated VARCHAR(255), PRIMARY KEY (word));
DELETE FROM ASSOCIATION;
exit;
