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

package org.apache.aries.cdi.container.internal.configuration;

public class ConfigurationCallback {

	public static enum State {STARTED, ADDED, UPDATED, REMOVED}

/*	public static class Builder {

		public ConfigurationCallback build() {
			//Objects.requireNonNull(_pid);
			Objects.requireNonNull(_policy);
			return new ConfigurationCallback(_pid, _policy, _onAdd, _onUpdate, _onRemove);
		}

		public Builder onAdd(Consumer<ConfigurationCallback> onAdd) {
			_onAdd = onAdd;
			return this;
		}

		public Builder onRemove(Consumer<ConfigurationCallback> onRemove) {
			_onRemove = onRemove;
			return this;
		}

		public Builder onUpdate(Consumer<ConfigurationCallback> onUpdate) {
			_onUpdate = onUpdate;
			return this;
		}

		public Builder pid(String pid) {
			_pid = pid;
			return this;
		}

		public Builder policy(ConfigurationPolicy policy) {
			_policy = policy;
			return this;
		}

		private String _pid;
		private ConfigurationPolicy _policy;
		private Consumer<ConfigurationCallback> _onAdd;
		private Consumer<ConfigurationCallback> _onRemove;
		private Consumer<ConfigurationCallback> _onUpdate;

	}

	private ConfigurationCallback(
		String pid,
		ConfigurationPolicy policy,
		Consumer<ConfigurationCallback> onAdd,
		Consumer<ConfigurationCallback> onUpdate,
		Consumer<ConfigurationCallback> onRemove) {

		_pid = pid;
		_policy = policy;
		_onAdd = Optional.ofNullable(onAdd);
		_onRemove = Optional.ofNullable(onRemove);
		_onUpdate = Optional.ofNullable(onUpdate);
		_state = State.STARTED;
	}

	public void added(Dictionary<String, ?> properties) {
		if (_policy == ConfigurationPolicy.IGNORE) return;
		assertNotEmpty(properties);
		switch (_state) {
			case STARTED:
			case REMOVED:
				_properties = properties;
				_state = State.ADDED;
				_onAdd.ifPresent(f -> f.accept(this));
				break;
			default:
				throw new IllegalStateException(_state.toString());
		}
	}

	@SuppressWarnings("incomplete-switch")
	public void init() {
		switch (_policy) {
			case DEFAULT:
			case IGNORE:
			case OPTIONAL:
				_onAdd.ifPresent(f -> f.accept(this));
				break;
		}
	}

	public ConfigurationPolicy policy() {
		return _policy;
	}

	public Dictionary<String, ?> properties() {
		if (_policy == ConfigurationPolicy.IGNORE) return NULL_PROPERTIES;
		return _properties;
	}

	public void removed() {
		if (_policy == ConfigurationPolicy.IGNORE) return;
		switch (_state) {
			case ADDED:
			case UPDATED:
				_properties = NULL_PROPERTIES;
				_state = State.REMOVED;
				_onRemove.ifPresent(f -> f.accept(this));
				break;
			default:
				throw new IllegalStateException(_state.toString());
		}
	}

	public boolean resolved() {
		if ((_policy == ConfigurationPolicy.REQUIRE) && (_properties == NULL_PROPERTIES)) {
			return false;
		}

		return true;
	}

	public State state() {
		return _state;
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = String.format("ConfigurationCallback[pid='%s', policy='%s', state='%s', properties='%s']", _pid, _policy, _state, properties());
		}
		return _string;
	}

	public void updated(Dictionary<String, ?> properties) {
		if (_policy == ConfigurationPolicy.IGNORE) return;
		assertNotEmpty(properties);
		switch (_state) {
			case ADDED:
			case UPDATED:
				_properties = properties;
				_state = State.UPDATED;
				_onUpdate.ifPresent(f -> f.accept(this));
				break;
			default:
				throw new IllegalStateException(_state.toString());
		}
	}

	private static final void assertNotEmpty(Dictionary<String, ?> dictionary) {
		if ((dictionary == null) || dictionary.isEmpty()) {
			throw new IllegalArgumentException("Empty properties");
		}
	}

	private static final Dictionary<String, ?> NULL_PROPERTIES = new Hashtable<>();

	private final String _pid;
	private final ConfigurationPolicy _policy;
	private volatile Dictionary<String, ?> _properties = NULL_PROPERTIES;
	private final Optional<Consumer<ConfigurationCallback>> _onAdd;
	private final Optional<Consumer<ConfigurationCallback>> _onUpdate;
	private final Optional<Consumer<ConfigurationCallback>> _onRemove;
	private volatile State _state;
	private volatile String _string;
*/
}
