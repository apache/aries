/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal.container;

import java.util.function.Predicate;

import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

public interface CheckedCallback<T, R> extends Failure, Predicate<Op>, Success<T, R> {

	@Override
	@SuppressWarnings("unchecked")
	public default Promise<R> call(Promise<T> resolved) throws Exception {
		return (Promise<R>)resolved;
	}

	@Override
	public default void fail(Promise<?> resolved) throws Exception {
		resolved.getFailure().printStackTrace();
	}

}
