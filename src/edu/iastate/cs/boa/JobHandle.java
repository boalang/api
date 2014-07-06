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

import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * Represents a handle to a job.  Can not be created, only returned
 * from Boa API calls.
 *
 * @author rdyer
 */
public final class JobHandle {
	private final BoaClient client;

	private final int id;
	public final int getId() { return id; }

	private final Date date;
	public final Date getDate() { return date; }

	private final InputHandle dataset;
	public final InputHandle getDataset() { return dataset; }

	private final String compiler;
	public final String getCompilerStatus() { return compiler; }

	private final String hadoop;
	public final String getHadoopStatus() { return hadoop; }

	JobHandle(final BoaClient client, final int id, final Date date, final InputHandle dataset, final String compiler, final String hadoop) {
		this.client = client;
		this.id = id;
		this.date = date;
		this.dataset = dataset;
		this.compiler = compiler;
		this.hadoop = hadoop;
	}

	@Override
	public final String toString() {
		return id + " (" + date + ") - " + dataset + " - compiler_status(" + compiler + ") hadoop_status(" + hadoop + ")";
	}

	/**
	 * Stops the job, if it is running.
	 *
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public void stop() throws BoaException, NotLoggedInException {
		client.stop(id);
	}

	/**
	 * Resubmits the job.
	 *
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public void resubmit() throws BoaException, NotLoggedInException {
		client.resubmit(id);
	}

	/**
	 * Deletes the job.
	 *
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public void delete() throws BoaException, NotLoggedInException {
		client.delete(id);
	}

	/**
	 * Marks a job as public/private.
	 *
	 * @param isPublic should the job be public (true) or private (false)
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public void setPublic(final boolean isPublic) throws BoaException, NotLoggedInException {
		client.setPublic(id, isPublic);
	}

	/**
	 * Get the job's public/private status.
	 *
	 * @return <code>true</code> if the job is public, else <code>false</code>
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public boolean getPublic() throws BoaException, NotLoggedInException {
		return client.getPublic(id);
	}

	/**
	 * Get the job's URL.
	 *
	 * @return a {@link java.net.URL} to view the job
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public URL getUrl() throws BoaException, NotLoggedInException {
		return client.getUrl(id);
	}

	/**
	 * Get the job's public page URL.
	 *
	 * @return a {@link java.net.URL} to view the job's public page
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public URL getPublicUrl() throws BoaException, NotLoggedInException {
		return client.getPublicUrl(id);
	}

	/**
	 * Return any errors from trying to compile the job.
	 *
	 * @return a (possibly empty) {@link java.util.List} of compiler error messages
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public List<String> getCompilerErrors() throws BoaException, NotLoggedInException {
		return client.getCompilerErrors(id);
	}

	/**
	 * Return the source query for this job.
	 *
	 * @return the source query for this job
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public String getSource() throws BoaException, NotLoggedInException {
		return client.getSource(id);
	}
}
