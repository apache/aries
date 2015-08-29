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
import java.util.Set;


public abstract class AbstractClauseBasedHeader<C extends Clause>
        implements Header<C> {

    protected final Set<C> clauses = new HashSet<C>();

    protected AbstractClauseBasedHeader(){}
    
    public AbstractClauseBasedHeader(Collection<C> clauses) {
        if (clauses.isEmpty())
            throw new IllegalArgumentException(String.format(
                    "The header %s must have at least one clause.", getName()));
        this.clauses.addAll(clauses);
    }

    public AbstractClauseBasedHeader(String clausesStr) {
        this.clauses.addAll(processHeader(clausesStr));
        if (clauses.isEmpty())
            throw new IllegalArgumentException(String.format(
                    "The header %s must have at least one clause.", getName()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractClauseBasedHeader<?> other = (AbstractClauseBasedHeader<?>) obj;
        if (getName() == null) {
            if (other.getName() != null)
                return false;
        } else
            if (!getName().equals(other.getName()))
                return false;
        if (clauses == null) {
            if (other.clauses != null)
                return false;
        } else
            if (!clauses.equals(other.clauses))
                return false;
        return true;
    }

    @Override
    public final Set<C> getClauses() {
        return Collections.unmodifiableSet(clauses);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result
                + ((getValue() == null) ? 0 : getValue().hashCode());
        result = prime * result + ((clauses == null) ? 0 : clauses.hashCode());
        return result;
    }

    protected abstract Collection<C> processHeader(String header);

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
