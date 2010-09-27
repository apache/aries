connect 'jdbc:derby:target/wordsDB/;create=true';
CREATE TABLE Association (word VARCHAR(255) NOT NULL, associated VARCHAR(255), PRIMARY KEY (word));
DELETE FROM Association;
exit;
