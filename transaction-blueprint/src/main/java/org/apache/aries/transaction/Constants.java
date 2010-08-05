package org.apache.aries.transaction;

import java.util.regex.Pattern;

public class Constants {
    public static final Pattern WILDCARD = Pattern.compile("\\Q.*\\E");
    public static final String BEAN = "bean";
    public static final String VALUE = "value";
    public static final String METHOD = "method";
    public static final String TX11_SCHEMA = "transactionv11.xsd";
    public static final String TX10_SCHEMA = "transactionv10.xsd";
    
	public final static String TRANSACTION10URI = "http://aries.apache.org/xmlns/transactions/v1.0.0";
	public final static String TRANSACTION11URI = "http://aries.apache.org/xmlns/transactions/v1.1.0";
}
