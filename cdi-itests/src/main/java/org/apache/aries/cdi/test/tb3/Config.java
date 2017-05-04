package org.apache.aries.cdi.test.tb3;

@interface Config {

	String color() default "blue";

	int[] ports() default 35777;

}