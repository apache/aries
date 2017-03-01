package org.apache.aries.cdi.container.internal.model;

import static org.apache.aries.cdi.container.internal.model.Constants.BEAN_CLASS_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.CDI10_URI;
import static org.apache.aries.cdi.container.internal.model.Constants.TARGET_ATTRIBUTE;

import org.xml.sax.Attributes;

public class ReferenceModel extends AbstractModel {

	public ReferenceModel(Attributes attributes) {
		_beanClass = getValue(CDI10_URI, BEAN_CLASS_ATTRIBUTE, attributes);
		_target = getValue(CDI10_URI, TARGET_ATTRIBUTE, attributes);
	}

	public String getBeanClass() {
		return _beanClass;
	}

	public String getTarget() {
		return _target;
	}

	private final String _beanClass;
	private final String _target;

}
