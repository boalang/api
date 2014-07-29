/*
 * Copyright 2014, Robert Dyer,
 *                 and Bowling Green State University
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

import java.io.Serializable;

/**
 * Represents a handle to an input dataset.  Can not be created, only returned
 * from Boa API calls.
 *
 * @author rdyer
 */
public final class InputHandle implements Serializable {
	private static final long serialVersionUID = 8396340497942252663L;

	private final int id;
	/**
	 * Returns the input dataset's unique identifier.
	 *
	 * @return the input dataset's id
	 */
	public final int getId() { return id; }

	private final String name;
	/**
	 * Returns the human-readable name of the input dataset.
	 *
	 * @return the input dataset's name
	 */
	public final String getName() { return name; }

	InputHandle(final int id, final String name) throws BoaException {
		this.id = id;
		this.name = name;
	}

	/** {@inheritDoc} */
	@Override
	public final String toString() {
		return id + ", " + name;
	}
}
