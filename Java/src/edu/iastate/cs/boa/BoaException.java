/*
 * Copyright 2014, Robert Dyer.
 *
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
package edu.iastate.cs.boa;

/**
 * The base class for any exception thrown by the Boa API.
 *
 * @author rdyer
 */
public class BoaException extends Exception {
	private static final long serialVersionUID = 978922343860883155L;

	BoaException() {
		super();
	}

	BoaException(final String msg) {
		super(msg);
	}

	BoaException(final String msg, final Exception e) {
		super(msg, e);
	}
}
