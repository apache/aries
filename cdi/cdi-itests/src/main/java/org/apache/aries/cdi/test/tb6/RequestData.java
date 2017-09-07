package org.apache.aries.cdi.test.tb6;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.service.cdi.annotations.Reference;

@RequestScoped
public class RequestData implements Serializable {

	public boolean hasData() {
		return data != null;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = pojo.foo(data);
	}

	private static final long serialVersionUID = 1L;
	private String data;

	@Inject
	@Reference
	Pojo pojo;

}
