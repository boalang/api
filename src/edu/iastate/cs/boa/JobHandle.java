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

import java.util.Date;

/**
 * Represents a handle to a job.  Can not be created, only returned
 * from Boa API calls.
 *
 * @author rdyer
 */
public final class JobHandle {
	private final long id;
	public final long getId() { return id; }

	private final Date date;
	public final Date getDate() { return date; }

	private final InputHandle dataset;
	public final InputHandle getDataset() { return dataset; }

	private final String compiler;
	public final String getCompilerStatus() { return compiler; }

	private final String hadoop;
	public final String getHadoopStatus() { return hadoop; }

	JobHandle(final long id, final Date date, final InputHandle dataset, final String compiler, final String hadoop) {
		this.id = id;
		this.date = date;
		this.dataset = dataset;
		this.compiler = compiler;
		this.hadoop = hadoop;
	}

	public final String toString() {
		return id + " (" + date + ") - " + dataset + " - compiler_status(" + compiler + ") hadoop_status(" + hadoop + ")";
	}
}
