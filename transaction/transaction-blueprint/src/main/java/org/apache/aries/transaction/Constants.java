package org.apache.aries.transaction;

import java.util.regex.Pattern;

public class Constants {
    public static final Pattern WILDCARD = Pattern.compile("\\Q.*\\E");
    public static final String BEAN = "bean";
    public static final String VALUE = "value";
    public static final String METHOD = "method";
    public static final String TX_SCHEMA = "transaction.xsd";
}
