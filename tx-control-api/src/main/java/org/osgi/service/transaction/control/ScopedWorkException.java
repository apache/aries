/*
 * Copyright (c) OSGi Alliance (2016). All Rights Reserved.
 * 
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
package org.osgi.service.transaction.control;

import java.io.Serializable;

/**
 * An Exception that is thrown when a piece of scoped work exits with an
 * {@link Exception}
 */
public class ScopedWorkException extends RuntimeException {

	/**
	 */
	private static final long			serialVersionUID	= 4160254161503114842L;

	/**
	 * {@link TransactionContext} instances are not required to 
	 * be {@link Serializable}, and the ongoing context is very 
	 * unlikely to be active post deserialization. As a result
	 * this field is transient.
	 */
	private transient final TransactionContext context;

	/**
	 * Creates a new TransactionException with the supplied message and cause
	 * 
	 * @param message
	 * @param cause
	 * @param context 
	 */
	public ScopedWorkException(String message, Throwable cause, TransactionContext context) {
		super(message, cause);
		this.context = context;
	}

	/**
	 * @return The ongoing transaction context if the current scope was still 
	 *         active when this exception was raised or <code>null</code> otherwise.
	 *         Note that this property will not be persisted during serialization.
	 */
	public TransactionContext ongoingContext() {
		return context;
	}

	/**
	 * @return The cause of this Exception as a {@link RuntimeException} if it
	 *         is one, or this otherwise
	 */
	public RuntimeException asRuntimeException() {
		return (RuntimeException) getCause();
	}

	/**
	 * Throws the cause of this Exception as a RuntimeException, or as the supplied
	 * Exception type.
	 * <p>
	 * Usage is of the form:
	 * 
	 * <pre>
	 * public void doStuff() throws IOException {
	 *     try {
	 *         ...
	 *     } catch (ScopedWorkException swe) {
	 *         throw swe.as(IOException.class);
	 *     }
	 * }
	 * </pre>
	 * 
	 * @param throwable The exception type to throw
	 *        
	 * @return This method will always throw an exception, either:
	 *           <ul>
	 *             <li>The cause of this exception as the type &lt;T&gt;</li>
	 *             <li>The cause of this exception as a runtime exception</li>
	 *             <li>An IllegalArgumentException with its cause set to <code>this</code></li>
	 *           </ul>
	 * 
	 * @throws T the type of exception to be thrown
	 * @throws IllegalArgumentException if the cause is not a {@link RuntimeException} or of type T
	 */
	public <T extends Throwable> T as(Class<T> throwable) throws T, IllegalArgumentException {
		Throwable t = getCause();

		if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}

		possibleThrow(throwable, t);

		throw new IllegalArgumentException(
				"The cause of this Exception is not an instance of "
						+ throwable.getName(),
				this);
	}

	/**
	 * Throws the cause of this Exception as a RuntimeException, or as one of the
	 * supplied Exception types.
	 * <p>
	 * Usage is of the form:
	 * 
	 * <pre>
	 * public void doStuff() throws IOException, ClassNotFoundException {
	 *     try {
	 *         ...
	 *     } catch (ScopedWorkException swe) {
	 *         throw swe.asOneOf(IOException.class, ClassNotFoundException.class);
	 *     }
	 * }
	 * </pre>
	 * 
	 * @param a The first possible exception type to throw
	 * @param b The second possible exception type to throw
	 * @return This method will always throw an exception, either:
	 *           <ul>
	 *             <li>The cause of this exception as the type &lt;A&gt;</li>
	 *             <li>The cause of this exception as the type &lt;B&gt;</li>
	 *             <li>The cause of this exception as a runtime exception</li>
	 *             <li>An IllegalArgumentException with its cause set to <code>this</code></li>
	 *           </ul>
	 * @throws A The first possible exception type to throw
	 * @throws B The second possible exception type to throw
	 * @throws IllegalArgumentException if the cause is not a {@link RuntimeException} or of type A or B
	 */
	public <A extends Throwable, B extends Throwable> RuntimeException asOneOf(
			Class<A> a, Class<B> b) throws A, B, IllegalArgumentException {
		Throwable t = getCause();

		if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}

		possibleThrow(a, t);
		possibleThrow(b, t);

		throw new IllegalArgumentException(
				"The cause of this Exception is not an instance of "
						+ String.join(", ", a.getName(), b.getName()),
				this);
	}

	/**
	 * Throws the cause of this Exception as a RuntimeException or one of the
	 * supplied Exception types.
	 * 
	 * @see #asOneOf(Class, Class)
	 * @param a The first possible exception type to throw
	 * @param b The second possible exception type to throw
	 * @param c The third possible exception type to throw
	 * @return This method will always throw an exception, either:
	 *           <ul>
	 *             <li>The cause of this exception as the type &lt;A&gt;</li>
	 *             <li>The cause of this exception as the type &lt;B&gt;</li>
	 *             <li>The cause of this exception as the type &lt;C&gt;</li>
	 *             <li>The cause of this exception as a runtime exception</li>
	 *             <li>An IllegalArgumentException with its cause set to <code>this</code></li>
	 *           </ul>
	 * @throws A The first possible exception type to throw
	 * @throws B The second possible exception type to throw
	 * @throws C The third possible exception type to throw
	 * @throws IllegalArgumentException if the cause is not a {@link RuntimeException} or one of types A, B or C
	 */
	public <A extends Throwable, B extends Throwable, C extends Throwable> RuntimeException asOneOf(
			Class<A> a, Class<B> b, Class<C> c) throws A, B, C, IllegalArgumentException {
		Throwable t = getCause();

		if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}

		possibleThrow(a, t);
		possibleThrow(b, t);
		possibleThrow(c, t);

		throw new IllegalArgumentException(
				"The cause of this Exception is not an instance of " + String
						.join(", ", a.getName(), b.getName(), c.getName()),
				this);
	}

	/**
	 * Throws the cause of this Exception as a RuntimeException or one of the
	 * supplied Exception types.
	 * 
	 * @see #asOneOf(Class, Class)
	 * @param a The first possible exception type to throw
	 * @param b The second possible exception type to throw
	 * @param c The third possible exception type to throw
	 * @param d The fourth possible exception type to throw
	 * @return This method will always throw an exception, either:
	 *           <ul>
	 *             <li>The cause of this exception as the type &lt;A&gt;</li>
	 *             <li>The cause of this exception as the type &lt;B&gt;</li>
	 *             <li>The cause of this exception as the type &lt;C&gt;</li>
	 *             <li>The cause of this exception as a runtime exception</li>
	 *             <li>An IllegalArgumentException with its cause set to <code>this</code></li>
	 *           </ul>
	 * @throws A The first possible exception type to throw
	 * @throws B The second possible exception type to throw
	 * @throws C The third possible exception type to throw
	 * @throws D The fourth possible exception type to throw
	 * @throws IllegalArgumentException if the cause is not a {@link RuntimeException} or one of types A, B, C or D
	 */
	public <A extends Throwable, B extends Throwable, C extends Throwable, D extends Throwable> RuntimeException asOneOf(
			Class<A> a, Class<B> b, Class<C> c, Class<D> d) throws A, B, C, D, IllegalArgumentException {
		Throwable t = getCause();

		if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}

		possibleThrow(a, t);
		possibleThrow(b, t);
		possibleThrow(c, t);
		possibleThrow(d, t);

		throw new IllegalArgumentException(
				"The cause of this Exception is not an instance of " + String
						.join(", ", a.getName(), b.getName(), c.getName()),
				this);
	}

	@SuppressWarnings("unchecked")
	private <X extends Throwable> void possibleThrow(Class<X> x, Throwable t)
			throws X {
		if (x.isInstance(t)) {
			throw (X) t;
		}
	}

}
