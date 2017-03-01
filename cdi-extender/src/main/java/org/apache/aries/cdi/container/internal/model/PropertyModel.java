package org.apache.aries.cdi.container.internal.model;

import static org.apache.aries.cdi.container.internal.model.Constants.CDI10_URI;
import static org.apache.aries.cdi.container.internal.model.Constants.NAME_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.TYPE_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.VALUE_ATTRIBUTE;

import org.osgi.service.cdi.annotations.PropertyType;
import org.xml.sax.Attributes;

public class PropertyModel extends AbstractModel {

	public PropertyModel(Attributes attributes) {
		_name = getValue(CDI10_URI, NAME_ATTRIBUTE, attributes);
		_typeString = getValue(CDI10_URI, TYPE_ATTRIBUTE, attributes);
		_type = PropertyType.valueOf(_typeString);
		_value = new String[] {getValue(CDI10_URI, VALUE_ATTRIBUTE, attributes)};
	}

	public String getName() {
		return _name;
	}

	public PropertyType getType() {
		return _type;
	}

	public String[] getValue() {
		if (_extraValue != null) {
			return _extraValue.toString().trim().split("\\s*\\n\\s*");
		}

		return _value;
	}

	public void appendValue(String value) {
		if (_extraValue == null) {
			_extraValue = new StringBuilder();
			_type = PropertyType.valueOf(_typeString + "_Array");
		}

		_extraValue.append(value);
	}

	private final String _name;
	private PropertyType _type;
	private final String _typeString;
	private String[] _value;
	private StringBuilder _extraValue;

}
