package org.apache.aries.cdi.container.internal.model;

import static org.apache.aries.cdi.container.internal.model.Constants.CDI10_URI;
import static org.apache.aries.cdi.container.internal.model.Constants.PID_ATTRIBUTE;

import org.xml.sax.Attributes;

public class ConfigurationModel extends AbstractModel {

	public ConfigurationModel(Attributes attributes) {
		_pid = getValue(CDI10_URI, PID_ATTRIBUTE, attributes);
	}

	public String getPid() {
		return _pid;
	}

	private final String _pid;

}
