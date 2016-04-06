package org.apache.aries.tx.control.itests.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Message {

	@Id
	@GeneratedValue
	public Integer id;
	
	public String message;

	@Override
	public String toString() {
		return "Message [id=" + id + ", message=" + message + "]";
	}
	
}
