/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.archive;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.subsystem.core.capabilityset.SimpleFilter;
import org.osgi.framework.VersionRange;

public abstract class AbstractClauseBasedHeader<C extends Clause> implements Header<C> {
	protected static VersionRange parseVersionRange(List<SimpleFilter> filters) {
		SimpleFilter floor = null;
		SimpleFilter ceiling = null;
		for (SimpleFilter filter : filters) {
			switch (filter.getOperation()) {
				case SimpleFilter.EQ:
				case SimpleFilter.GTE:
					floor = filter;
					break;
				case SimpleFilter.LTE:
					ceiling = filter;
					break;
				case SimpleFilter.NOT:
					SimpleFilter negated = ((List<SimpleFilter>)filter.getValue()).get(0);
					switch (negated.getOperation()) {
						case SimpleFilter.EQ:
						case SimpleFilter.GTE:
							ceiling = filter;
							break;
						case SimpleFilter.LTE:
							floor = filter;
							break;
						default:
							throw new IllegalArgumentException("Invalid filter: " + filter);
					}
					break;
				default:
					throw new IllegalArgumentException("Invalid filter: " + filter);
			}
		}
		if (ceiling == null) {
			return new VersionRange(String.valueOf(floor.getValue()));
		}
		String range = new StringBuilder()
			.append(floor.getOperation() == SimpleFilter.NOT ? '(' : '[')
			.append(floor.getOperation() == SimpleFilter.NOT ? 
					((List<SimpleFilter>)floor.getValue()).get(0).getValue() : floor.getValue())
			.append(',')
			.append(ceiling.getOperation() == SimpleFilter.NOT ? 
					((List<SimpleFilter>)ceiling.getValue()).get(0).getValue() : ceiling.getValue())
			.append(ceiling.getOperation() == SimpleFilter.NOT ? ')' : ']')
			.toString();
		return new VersionRange(range);
	}
	
	private static <C> Collection<C> computeClauses(String header, ClauseFactory<C> factory) {
		Collection<String> clauseStrs = new ClauseTokenizer(header).getClauses();
		Set<C> clauses = new HashSet<C>(clauseStrs.size());
		for (String clause : clauseStrs) {
			clauses.add(factory.newInstance(clause));
		}
		return clauses;
	}
	
	public interface ClauseFactory<C> {
		public C newInstance(String clause);
	}
	
    protected final Set<C> clauses;
    
    public AbstractClauseBasedHeader(Collection<C> clauses) {
        if (clauses.isEmpty()) {
            throw new IllegalArgumentException("No clauses");
        }
        this.clauses = Collections.synchronizedSet(new HashSet<C>(clauses));
    }

    public AbstractClauseBasedHeader(String header, ClauseFactory<C> factory) {
    	this(computeClauses(header, factory));
    }

    @Override
    public final Collection<C> getClauses() {
        return Collections.unmodifiableCollection(clauses);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + getName().hashCode();
        result = 31 * result + clauses.hashCode();
        return result;
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o == this) {
    		return true;
    	}
    	if (!(o instanceof AbstractClauseBasedHeader)) {
    		return false;
    	}
    	AbstractClauseBasedHeader<?> that = (AbstractClauseBasedHeader<?>)o;
    	return that.getName().equals(this.getName())
    			&& that.clauses.equals(this.clauses);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (C clause : getClauses()) {
            builder.append(clause).append(',');
        }
        // Remove the trailing comma. Note at least one clause is guaranteed to
        // exist.
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }
}
