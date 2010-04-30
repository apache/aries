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


/**
 * @author forrestxm
 *
 */

package org.apache.aries.samples.blueprint.idverifier.client;

import java.util.Set;

import org.apache.aries.samples.blueprint.idverifier.api.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;

public class PersonBankBean {
	private PersonalInfo personinfo;
	private BankInfo bankinfo;
	private String bankinfobeanid;	
	private CreditRecordOperation cro;
	
	private BlueprintContainer bpcontainer;
	private BundleContext bpbundlecontext;
	private ServiceRegistration svcreg4cro;
	
	public PersonBankBean(PersonalInfo info){
		this.personinfo = info;
	}
	
	/**
	 * @return the bankinfo
	 */
	public BankInfo getBankinfo() {
		return bankinfo;
	}

	/**
	 * @param bankinfo the bankinfo to set
	 */
	public void setBankinfo(BankInfo bankinfo) {
		this.bankinfo = bankinfo;
	}

	/**
	 * @return the bankinfobeanid
	 */
	public String getBankinfobeanid() {
		return bankinfobeanid;
	}

	/**
	 * @param bankinfobeanid the bankinfobeanid to set
	 */
	public void setBankinfobeanid(String bankinfobeanid) {
		this.bankinfobeanid = bankinfobeanid;
	}

	/**
	 * @return the bpcontainer
	 */
	public BlueprintContainer getBpcontainer() {
		return bpcontainer;
	}

	/**
	 * @param bpcontainer the bpcontainer to set
	 */
	public void setBpcontainer(BlueprintContainer bpcontainer) {
		this.bpcontainer = bpcontainer;
	}

	/**
	 * @return the cro
	 */
	public CreditRecordOperation getCro() {
		return cro;
	}

	/**
	 * @param cro the cro to set
	 */
	public void setCro(CreditRecordOperation cro) {
		this.cro = cro;
	}

	/**
	 * @return the svcreg4cro
	 */
	public ServiceRegistration getSvcreg4cro() {
		return svcreg4cro;
	}

	/**
	 * @param svcreg4cro the svcreg4cro to set
	 */
	public void setSvcreg4cro(ServiceRegistration svcreg4cro) {
		this.svcreg4cro = svcreg4cro;
	}

	/**
	 * @return the bpbundlecontext
	 */
	public BundleContext getBpbundlecontext() {
		return bpbundlecontext;
	}

	/**
	 * @param bpbundlecontext the bpbundlecontext to set
	 */
	public void setBpbundlecontext(BundleContext bpbundlecontext) {
		this.bpbundlecontext = bpbundlecontext;
	}

	public void startUp(){
		System.out.println("*******Start of Printing Personal Bank/Credit Information************");		
		this.personinfo.toString();
		
		// get component instance of BankInfo at runtime
		this.setBankinfo((BankInfo)bpcontainer.getComponentInstance(this.getBankinfobeanid()));
		this.bankinfo.toString();
		
		// get inlined service object from service registration object
		ServiceReference svcref = this.svcreg4cro.getReference();
		this.setCro((CreditRecordOperation)this.bpbundlecontext.getService(svcref));
		
		Set<String> allcreditrecords = cro.query(this.personinfo.getPersonid());
		if (allcreditrecords.isEmpty()){
			System.out.println("No credit records for id " + this.personinfo.getPersonid());
		} else {
			System.out.println("The credit records for id " + this.personinfo.getPersonid() + " are as follows:");
			for (String arecord : allcreditrecords){
				System.out.println(arecord);
			}
		}		
		System.out.println("*******End of Printing Personal Bank/Credit Information**************");
	}

}
