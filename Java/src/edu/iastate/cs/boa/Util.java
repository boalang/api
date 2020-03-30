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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Utilities for handling data from the server.
 *
 * @author rdyer
 */
final class Util {
	@SuppressWarnings("unchecked")
	final static JobHandle parseJob(final BoaClient client, final Map<?, ?> job) throws BoaException {
		verifyKeys(job, "id", "submitted", "input", "compiler_status", "hadoop_status");
		return new JobHandle(
			client,
			strToInt((String)job.get("id")),
			strToDate((String)job.get("submitted")),
			parseDataset((Map<String, Object>)job.get("input")),
			strToCompileStatus((String)job.get("compiler_status")),
			strToExecutionStatus((String)job.get("hadoop_status"))
		);
	}

	final static InputHandle parseDataset(final Map<?, ?> input) throws BoaException {
		verifyKeys(input, "id", "name");
		return new InputHandle(strToInt((String)input.get("id")), (String)input.get("name"));
	}

	private static void verifyKeys(final Map<?, ?> m, final String... keys) throws BoaException {
		for (final String k : keys)
			if (!m.containsKey(k))
				throw new BoaException("Invalid response from server: response does not contain key '" + k + "'.");
	}

	// e.g.: 2014-05-23 16:38:49 CDT
	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

	private static Date strToDate(final String s) throws BoaException {
		try {
			return df.parse(s);
		} catch (final ParseException e) {
			throw new BoaException("Invalid date '" + s + "' from server.", e);
		}
	}

	private static int strToInt(final String s) throws BoaException {
		try {
			return Integer.parseInt(s);
		} catch (final NumberFormatException e) {
			throw new BoaException("Invalid number '" + s + "' from server.", e);
		}
	}

	private static CompileStatus strToCompileStatus(final String s) throws BoaException {
		if ("Error".equals(s))
			return CompileStatus.ERROR;
		if ("Finished".equals(s))
			return CompileStatus.FINISHED;
		if ("Running".equals(s))
			return CompileStatus.RUNNING;
		if ("Waiting".equals(s))
			return CompileStatus.WAITING;
		throw new BoaException("Invalid response from server: compile_status '" + s + "' unknown");
	}

	private static ExecutionStatus strToExecutionStatus(final String s) throws BoaException {
		if ("Error".equals(s))
			return ExecutionStatus.ERROR;
		if ("Finished".equals(s))
			return ExecutionStatus.FINISHED;
		if ("Running".equals(s))
			return ExecutionStatus.RUNNING;
		if ("Waiting".equals(s))
			return ExecutionStatus.WAITING;
		throw new BoaException("Invalid response from server: execution_status '" + s + "' unknown");
	}
}
