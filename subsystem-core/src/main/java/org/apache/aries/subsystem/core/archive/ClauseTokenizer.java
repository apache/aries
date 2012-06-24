package org.apache.aries.subsystem.core.archive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ClauseTokenizer {
	private final Collection<String> clauses = new ArrayList<String>();
	
	public ClauseTokenizer(String value) {
		int numOfChars = value.length();
		StringBuilder builder = new StringBuilder(numOfChars);
		int numOfQuotes = 0;
		for (char c : value.toCharArray()) {
			numOfChars--;
			if (c == ',') {
				if (numOfQuotes % 2 == 0) {
					clauses.add(builder.toString());
					builder = new StringBuilder(numOfChars);
				}
				else
					builder.append(c);
			}
			else if (c == '"')
				numOfQuotes++;
			else
				builder.append(c);
		}
		clauses.add(builder.toString());
	}
	
	public Collection<String> getClauses() {
		return Collections.unmodifiableCollection(clauses);
	}
}
