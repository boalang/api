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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Represents a handle to a job.  Can not be created, only returned
 * from Boa API calls.
 *
 * @author rdyer
 */
public final class JobHandle implements Serializable {
	private static final long serialVersionUID = 6601705556435705094L;

	private final BoaClient client;

	private final int id;
	/**
	 * Returns the job's unique identifier.
	 *
	 * @return the job's id
	 */
	public final int getId() { return id; }

	private Date date;
	/**
	 * Returns the {@link Date} the job was last submitted.
	 *
	 * @return the last submitted {@link Date}
	 */
	public final Date getDate() { return date; }

	private final InputHandle dataset;
	/**
	 * Returns the input dataset the job queried.
	 *
	 * @return an {@link InputHandle} to the input dataset queried
	 */
	public final InputHandle getDataset() { return dataset; }

	private CompileStatus compilerStatus;
	/**
	 * Returns the compiler status for the job.
	 *
	 * @return the job's compiler status
	 */
	public final CompileStatus getCompilerStatus() { return compilerStatus; }

	private ExecutionStatus execStatus;
	/**
	 * Returns the execution status for the job.
	 *
	 * @return the job's execution status
	 */
	public final ExecutionStatus getExecutionStatus() { return execStatus; }

	JobHandle(final BoaClient client, final int id, final Date date, final InputHandle dataset, final CompileStatus compilerStatus, final ExecutionStatus execStatus) {
		this.client = client;
		this.id = id;
		this.date = date;
		this.dataset = dataset;
		this.compilerStatus = compilerStatus;
		this.execStatus = execStatus;
	}

	/** {@inheritDoc} */
	@Override
	public final String toString() {
		return id + " (" + date + ") - " + dataset + " - compiler_status(" + compilerStatus + ") execution_status(" + execStatus + ")";
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
	 * @param isPublic should the job be public (<code>true</code>) or private (<code>false</code>)
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
	 * <b>Note that this will return a URL even if the job is not marked public.</b>
	 * Make sure to call {@link #getPublic()} to verify the URL is valid.
	 * @see #getPublic()
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

	/**
	 * Return the output for this job, if it finished successfully and has output.
	 *
	 * @return the output for this job
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public String getOutput() throws BoaException, NotLoggedInException {
		final File f = new File(UUID.randomUUID().toString());
		this.getOutput(f);

		StringBuffer sb = new StringBuffer();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(f));

			char[] buf = new char[4096];
			int len = 0;
			while ((len = br.read(buf, 0, 4096)) > 0) {
				sb.append(buf, 0, len);
			}
		} catch (final IOException e) {
			// ignore
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (final IOException e) {
				// ignore
			}
		}

		try {
			f.delete();
		} catch (final Exception e) { }

		return sb.toString();
	}

	/**
     * Store the output for this job into a {@link java.io.File}, if it
     * finished successfully and has output.
	 *
	 * @param f where to store the file
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public void getOutput(final File f) throws BoaException, NotLoggedInException {
		client.getOutput(id, f);
	}

	/**
	 * Return a subset of the output for this job, if it finished successfully and has output.
	 *
	 * @param start the starting offset
	 * @param len the length of the output
	 * @return the output for this job
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public String getOutput(final long start, final long len) throws BoaException, NotLoggedInException {
		return client.getOutput(id, start, len);
	}

	/**
	 * Return size of the output for this job, if it finished successfully and has output.
	 *
	 * @return the size of the output for this job
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public int getOutputSize() throws BoaException, NotLoggedInException {
		return client.getOutputSize(id);
	}

	/**
	 * Refreshes the cached data for this job.
	 *
	 * @throws BoaException if the command fails for any reason
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public void refresh() throws BoaException, NotLoggedInException {
		final JobHandle j = client.getJob(id);

		this.date = j.getDate();
		this.compilerStatus = j.getCompilerStatus();
		this.execStatus = j.getExecutionStatus();
	}
}
