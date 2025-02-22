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
package org.apache.aries.samples.blueprint.idverifier.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author forrestxm
 *
 */
public class BankInfo {
    private static final Logger log = LoggerFactory.getLogger(BankInfo.class);

	private String bankname;
	private String bankaddress;
	private String banklegalpersonname;
	private String bankregistrationnumber;
	
	/**
	 * @return the bankname
	 */
	public String getBankname() {
		return bankname;
	}
	/**
	 * @param bankname the bankname to set
	 */
	public void setBankname(String bankname) {
		this.bankname = bankname;
	}
	/**
	 * @return the bandaddress
	 */
	public String getBankaddress() {
		return bankaddress;
	}
	/**
	 * @param bankaddress the bandaddress to set
	 */
	public void setBankaddress(String bankaddress) {
		this.bankaddress = bankaddress;
	}
	/**
	 * @return the banklegalpersonname
	 */
	public String getBanklegalpersonname() {
		return banklegalpersonname;
	}
	/**
	 * @param banklegalpersonname the banklegalpersonname to set
	 */
	public void setBanklegalpersonname(String banklegalpersonname) {
		this.banklegalpersonname = banklegalpersonname;
	}
	/**
	 * @return the bankregistrationnumber
	 */
	public String getBankregistrationnumber() {
		return bankregistrationnumber;
	}
	/**
	 * @param bankregistrationnumber the bankregistrationnumber to set
	 */
	public void setBankregistrationnumber(String bankregistrationnumber) {
		this.bankregistrationnumber = bankregistrationnumber;
	}
	@Override
	public String toString(){
		log.info("********Start of Printing Bank Info**********");
		log.info("Bank Name: " + this.getBankname());
		log.info("Bank Address: " + this.getBankaddress());
		log.info("Bank Legal Person: "+ this.getBanklegalpersonname());
		log.info("Bank Reg. Number: "+ this.getBankregistrationnumber());
		log.info("********End of Printing Bank Info**********");
		String delimiter = ",";
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		sb.append("bankname=" + this.getBankname()+ delimiter);
		sb.append("bankaddress=" + this.getBankaddress() + delimiter);
		sb.append("banklegalpersonname="+ this.getBanklegalpersonname() + delimiter);
		sb.append("bankregistrationnumber="+ this.getBankregistrationnumber());
		sb.append("]");
		return sb.toString();
	}

}
