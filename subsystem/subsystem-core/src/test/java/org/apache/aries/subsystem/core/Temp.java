package org.apache.aries.subsystem.core;

import java.util.regex.Pattern;

import org.apache.aries.subsystem.core.archive.Clause;
import org.apache.aries.subsystem.core.archive.Grammar;
import org.apache.aries.subsystem.core.archive.Parameter;
import org.apache.aries.subsystem.core.archive.ProvisionResourceHeader;

public class Temp {
	public static void main(String[] args) {
		String s = "org.eclipse.osgi; singleton:=true;deployed-version=3.7.0.v20110221;type=osgi.bundle";
		System.out.println(Pattern.matches(Grammar.CLAUSE, s));
		ProvisionResourceHeader h = new ProvisionResourceHeader(s);
		System.out.println(h.getName());
		for (Clause c : h.getClauses()) {
			System.out.println(c.getPath());
			for (Parameter p : c.getParameters()) {
				System.out.println(p.getValue());
			}
		}
	}
}
