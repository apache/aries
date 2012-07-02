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
					addClause(builder.toString().trim());
					builder = new StringBuilder(numOfChars);
					continue;
				}
			}
			else if (c == '"')
				numOfQuotes++;
			builder.append(c);
		}
		addClause(builder.toString().trim());
	}
	
	public Collection<String> getClauses() {
		return Collections.unmodifiableCollection(clauses);
	}
	
	private void addClause(String clause) {
		if (clause.isEmpty())
			return;
		clauses.add(clause);
	}
}
