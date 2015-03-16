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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.*;

/**
 * A client class for accessing Boa's API.
 *
 * <p>
 * To use this class, first construct an object by either using the default
 * constructor (to use the standard API domain/path) or providing the
 * domain/path to the API.  Then call {@link #login(java.lang.String username, java.lang.String password)}
 * with your Boa username and password to log into the remote API.
 * </p>
 *
 * <p>
 * When finished with the object, {@link #close()} must be called to log out of
 * the remote API.  The class is also {@link AutoCloseable} for use in a
 * try-with-resources block.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * try (final BoaClient client = new BoaClient()) {
 *   client.login("user", "pass");
 *
 *   // print all available input datasets
 *   for (final InputHandle d : client.getDatasets())
 *     System.out.println(d);
 *
 *   // print info about the last job submitted
 *   System.out.println("Last job submitted: " + client.getLastJob());
 * }
 * </pre>
 *
 * @author rdyer
 */
public class BoaClient implements AutoCloseable {
	private static final String BOA_DOMAIN = "boa.cs.iastate.edu";
	private static final String BOA_PATH   = "/boa/?q=boa/api";

	protected static final String METHOD_SYSTEM_CONNECT = "system.connect";

	protected static final String METHOD_USER_LOGIN  = "user.login";
	protected static final String METHOD_USER_LOGOUT = "user.logout";
	protected static final String METHOD_USER_TOKEN  = "user.token";

	protected static final String METHOD_BOA_DATASETS   = "boa.datasets";
	protected static final String METHOD_BOA_JOB        = "boa.job";
	protected static final String METHOD_BOA_JOBS       = "boa.jobs";
	protected static final String METHOD_BOA_JOBS_COUNT = "boa.count";
	protected static final String METHOD_BOA_JOBS_RANGE = "boa.range";
	protected static final String METHOD_BOA_SUBMIT     = "boa.submit";

	protected static final String METHOD_JOB_STOP            = "job.stop";
	protected static final String METHOD_JOB_RESUBMIT        = "job.resubmit";
	protected static final String METHOD_JOB_DELETE          = "job.delete";
	protected static final String METHOD_JOB_SET_PUBLIC      = "job.setpublic";
	protected static final String METHOD_JOB_PUBLIC          = "job.public";
	protected static final String METHOD_JOB_URL             = "job.url";
	protected static final String METHOD_JOB_PUBLIC_URL      = "job.publicurl";
	protected static final String METHOD_JOB_COMPILER_ERRORS = "job.compilerErrors";
	protected static final String METHOD_JOB_SOURCE          = "job.source";
	protected static final String METHOD_JOB_OUTPUT          = "job.output";
	protected static final String METHOD_JOB_PAGED_OUTPUT    = "job.pagedoutput";

	protected final XmlRpcClient xmlRpcClient = new XmlRpcClient();
	protected boolean loggedIn = false;

	/**
	 * Create a new Boa API client, using the standard domain/path.
	 */
	public BoaClient() {
		this(BOA_DOMAIN, BOA_PATH);
	}

	/**
	 * Create a new Boa API client by providing the domain/path to the API.
	 *
	 * @param domain the domain hosting the API (can not contain '/')
	 * @param path the path to the API (must start with '/')
	 */
	public BoaClient(final String domain, final String path) {
		if (domain.indexOf("/") != -1)
			throw new IllegalArgumentException("Argument 'domain' should not contain the protocol (http://) or a path (/).");
		if (path.indexOf("/") != 0)
			throw new IllegalArgumentException("Argument 'path' should start with '/'.");

		/*
		 * Endpoint is defined within Drupal services module configuration in order to define
		 * a URL that is available for serving a specific set of service calls. See drupal
		 * documentation for "Services 3.X". <a href="http://drupal.org/node/783236">http://drupal.org/node/783236</a>
		 */
		final String endpointURL = "http://" + domain + path;
		final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL(endpointURL));
		} catch (final MalformedURLException e) {
			// only happens if no/invalid protocol given, but we ensure this never happens
		}

		xmlRpcClient.setConfig(config);
	}

	/**
	 * Method to log into the remote API.
	 *
	 * @param username the Boa username to use to log in
	 * @param password the password for the user
	 * @throws LoginException if the login failed for any reason
	 */
	public void login(final String username, final String password) throws LoginException {
		if (loggedIn)
			return;

		loggedIn = false;

		try {
			@SuppressWarnings("unchecked")
			final Map<String, String> response = (Map<String, String>) xmlRpcClient.execute(METHOD_USER_LOGIN, new String[] { username, password });

			// construct a custom transport that sets the session cookie and CSRF token
			final String cookie = response.get("session_name") + "=" + response.get("sessid");
			final String token = (String)response.get("token");
			xmlRpcClient.setTransportFactory(new XmlRpcSunHttpTransportFactory(xmlRpcClient) {
				public XmlRpcTransport getTransport() {
					return new XmlRpcSunHttpTransport(xmlRpcClient) {
						@Override
						protected void initHttpHeaders(final XmlRpcRequest request) throws XmlRpcClientException {
							super.initHttpHeaders(request);
							setRequestHeader("Cookie", cookie);
							setRequestHeader("X-CSRF-Token", token);
						}
					};
				}
			});
		} catch (final XmlRpcHttpTransportException e) {
			throw new LoginException("Invalid path given to Boa API.", e);
		} catch (final XmlRpcException e) {
			if (e.getMessage().indexOf("Already logged in as ") == 0) {
				connect(username, password);
			} else {
				if (e.getMessage().indexOf("username") != -1)
					throw new LoginException("Invalid username or password.", e);
				if (e.getMessage().indexOf("response") != -1)
					throw new LoginException("Invalid domain given to Boa API.", e);
				if (e.getMessage().indexOf(":") != -1)
					throw new LoginException(e.getMessage().substring(e.getMessage().indexOf(":") + 2), e);
				throw new LoginException(e.getMessage(), e);
			}
		}

		loggedIn = true;
	}

	@SuppressWarnings("unchecked")
	protected void connect(final String username, final String password) throws LoginException {
		try {
			Map<String, String> response = (Map<String, String>) xmlRpcClient.execute(METHOD_SYSTEM_CONNECT, new String[] { username, password });
			final String cookie = response.get("session_name") + "=" + response.get("sessid");

			response = (Map<String, String>) xmlRpcClient.execute(METHOD_USER_TOKEN, new Object[] {});
			final String token = (String)response.get("token");

			// construct a custom transport that sets the session cookie and CSRF token
			xmlRpcClient.setTransportFactory(new XmlRpcSunHttpTransportFactory(xmlRpcClient) {
				public XmlRpcTransport getTransport() {
					return new XmlRpcSunHttpTransport(xmlRpcClient) {
						@Override
						protected void initHttpHeaders(final XmlRpcRequest request) throws XmlRpcClientException {
							super.initHttpHeaders(request);
							setRequestHeader("Cookie", cookie);
							setRequestHeader("X-CSRF-Token", token);
						}
					};
				}
			});
		} catch (final XmlRpcHttpTransportException e) {
			throw new LoginException("Invalid path given to Boa API.", e);
		} catch (final XmlRpcException e) {
			if (e.getMessage().indexOf("username") != -1)
				throw new LoginException("Invalid username or password.", e);
			if (e.getMessage().indexOf("response") != -1)
				throw new LoginException("Invalid domain given to Boa API.", e);
			if (e.getMessage().indexOf(":") != -1)
				throw new LoginException(e.getMessage().substring(e.getMessage().indexOf(":") + 2), e);
			throw new LoginException(e.getMessage(), e);
		}
	}

	/**
	 * Logs out of the Boa API.
	 *
	 * @throws BoaException if the logout fails for any reason
	 */
	public void close() throws BoaException {
		resetDatasetCache();
		try {
			loggedIn = false;
			xmlRpcClient.execute(METHOD_USER_LOGOUT, new Object[] {});
		} catch (final XmlRpcException e) {
			if (!"User is not logged in.".equals(e.getMessage()))
				throw new BoaException(e.getMessage(), e);
		}
	}

	/**
	 * Checks if the API is logged in and if not, throws an
	 * exception.
	 *
	 * @throws NotLoggedInException if the API is not logged in
	 */
	protected void ensureLoggedIn() throws NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();
	}

	protected List<InputHandle> datasetCache = null;
	protected long datasetCacheTime = 0;

	/**
	 * Resets the internal dataset cache.
	 */
	public void resetDatasetCache() {
		datasetCache = null;
		datasetCacheTime = 0;
	}

	/**
	 * Returns a list of available input datasets.  Since datasets rarely change, the results may
	 * be up to 1 day old.  The cache can be reset (see {@link #resetDatasetCache()}).
	 *
	 * @return a {@link java.util.Map} where keys are dataset IDs and values are their names
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public List<InputHandle> getDatasets() throws BoaException, NotLoggedInException {
		// cache results for 1 day
		if (datasetCache != null && datasetCacheTime + 86400000 > System.currentTimeMillis())
			return datasetCache;

		ensureLoggedIn();

		try {
			final Object[] result = (Object[]) xmlRpcClient.execute(METHOD_BOA_DATASETS, new Object[] {});

			datasetCache = new ArrayList<InputHandle>();
			for (int i = 0; i < result.length; i++)
				datasetCache.add(Util.parseDataset((Map<?, ?>)result[i]));

			datasetCacheTime = System.currentTimeMillis();

			return datasetCache;
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	/**
	 * Returns an array of available input dataset names.  Since datasets rarely change, the results may
	 * be up to 1 day old.  The cache can be reset (see {@link #resetDatasetCache()}).
	 *
	 * @return a {@link java.util.Map} where keys are dataset IDs and values are their names
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public String[] getDatasetNames() throws BoaException, NotLoggedInException {
		final List<InputHandle> list = getDatasets();
		final String[] items = new String[list.size()];

		for (int i = 0; i < list.size(); i++)
			items[i] = list.get(i).getName();

		return items;
	}

	/**
	 * Given the name of an input dataset, returns a handle (if one exists, otherwise <code>null</code>).
	 * Since datasets rarely change, the results may be up to 1 day old.  The cache can be reset
	 * (see {@link #resetDatasetCache()}).
	 *
	 * @param name the name of the input dataset to return a handle for
	 * @return an {@link InputHandle} for the specified dataset name
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public InputHandle getDataset(final String name) throws BoaException, NotLoggedInException {
		for (final InputHandle h : getDatasets())
			if (h.getName().equals(name))
				return h;

		return null;
	}

	/**
	 * Returns a specific job.
	 *
	 * @param id the jobs id
	 * @return a {@link JobHandle} for the job
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public JobHandle getJob(final int id) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			return Util.parseJob(this, (Map<?, ?>)xmlRpcClient.execute(METHOD_BOA_JOB, new Object[] {id}));
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	/**
	 * Returns the most recent job.
	 *
	 * @return a {@link JobHandle} for the latest job, or <code>null</code> if no jobs exist
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public JobHandle getLastJob() throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		final List<JobHandle> jobs = getJobList(0, 1);
		if (jobs.isEmpty())
			return null;

		return jobs.get(0);
	}

	/**
	 * Returns a list of the most recent jobs.  The number of jobs is limited based on the user's web setting.
	 * This includes public and private jobs.  Returned jobs are ordered from newest to oldest.
	 *
	 * @return a list of {@link JobHandle}s for the most recent jobs
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public List<JobHandle> getJobList() throws BoaException, NotLoggedInException {
		return getJobList(false);
	}

	/**
	 * Returns a list of the most recent jobs, based on an offset and length.
	 * This includes public and private jobs.  Returned jobs are ordered from newest to oldest.
	 *
	 * @param offset the starting offset
	 * @param length the number of jobs (at most) to return
	 * @return a list of {@link JobHandle}s for the jobs starting at the offset and containing at most length jobs
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public List<JobHandle> getJobList(final int offset, final int length) throws BoaException, NotLoggedInException {
		return getJobList(false, offset, length);
	}

	/**
	 * Returns a list of the most recent public (or all) jobs.  The number of jobs is limited based on the user's web setting.
	 * Returned jobs are ordered from newest to oldest.
	 *
	 * @param pubOnly if true, only return public jobs otherwise return all jobs
	 * @return a list of {@link JobHandle}s for the most recent jobs
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public List<JobHandle> getJobList(final boolean pubOnly) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			final Object[] result = (Object[])xmlRpcClient.execute(METHOD_BOA_JOBS, new Object[] {pubOnly});

			final List<JobHandle> jobs = new ArrayList<JobHandle>();
			for (int i = 0; i < result.length; i++)
				jobs.add(Util.parseJob(this, (Map<?, ?>)result[i]));

			return jobs;
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	/**
	 * Returns a list of the most recent public (or all) jobs, based on an offset and length.  Returned jobs are ordered from newest to oldest.
	 *
	 * @param pubOnly if true, only return public jobs otherwise return all jobs
	 * @param offset the starting offset
	 * @param length the number of jobs (at most) to return
	 * @return a list of {@link JobHandle}s for the jobs starting at the offset and containing at most length jobs
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public List<JobHandle> getJobList(final boolean pubOnly, final int offset, final int length) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			final Object[] result = (Object[])xmlRpcClient.execute(METHOD_BOA_JOBS_RANGE, new Object[] {pubOnly, offset, length});

			final List<JobHandle> jobs = new ArrayList<JobHandle>();
			for (int i = 0; i < result.length; i++)
				jobs.add(Util.parseJob(this, (Map<?, ?>)result[i]));

			return jobs;
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	/**
	 * Returns the number of jobs for the user.  This includes public and private jobs.
	 *
	 * @return the number of jobs the user has created
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public int getJobCount() throws BoaException, NotLoggedInException {
		return getJobCount(false);
	}

	/**
	 * Returns the number of public (or all) jobs for the user.
	 *
	 * @param pubOnly if true, return count of only public jobs otherwise return count of all jobs
	 * @return the number of jobs the user has created, possibly filtered to public only jobs
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public int getJobCount(final boolean pubOnly) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			return Integer.parseInt((String)xmlRpcClient.execute(METHOD_BOA_JOBS_COUNT, new Object[] {pubOnly}));
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	/**
	 * Submits a new query to Boa to query the specified and returns a handle to the new job.
	 *
	 * @param query the query source code
	 * @param dataset the input dataset to query
	 * @return a {@link JobHandle} for the new job
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public JobHandle query(final String query, final InputHandle dataset) throws BoaException, NotLoggedInException{
		ensureLoggedIn();

		try {
			return Util.parseJob(this, (Map<?, ?>)xmlRpcClient.execute(METHOD_BOA_SUBMIT, new Object[] { query, dataset.getId() }));
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	/**
	 * Submits a new query to Boa to query the latest (testing) dataset and returns a handle to the new job.
	 *
	 * @param query the query source code
	 * @return a {@link JobHandle} for the new job
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	public JobHandle query(final String query) throws BoaException, NotLoggedInException{
		ensureLoggedIn();

		try {
			return Util.parseJob(this, (Map<?, ?>)xmlRpcClient.execute(METHOD_BOA_SUBMIT, new Object[] { query, getDatasets().get(0).getId() }));
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// the methods below are not meant to be called by clients directly //
	// but rather through a handle                                      //
	//////////////////////////////////////////////////////////////////////

	void stop(final long id) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			xmlRpcClient.execute(METHOD_JOB_STOP, new Object[] { "" + id });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	void resubmit(final long id) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			xmlRpcClient.execute(METHOD_JOB_RESUBMIT, new Object[] { "" + id });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	void delete(final long id) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			xmlRpcClient.execute(METHOD_JOB_DELETE, new Object[] { "" + id });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	void setPublic(final long id, final boolean isPublic) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			xmlRpcClient.execute(METHOD_JOB_SET_PUBLIC, new Object[] { "" + id, isPublic });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	boolean getPublic(final long id) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			return (Integer)xmlRpcClient.execute(METHOD_JOB_PUBLIC, new Object[] { "" + id }) == 1;
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	URL getUrl(final long id) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			return new URL((String)xmlRpcClient.execute(METHOD_JOB_URL, new Object[] { "" + id }));
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		} catch (final MalformedURLException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	URL getPublicUrl(final long id) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			return new URL((String)xmlRpcClient.execute(METHOD_JOB_PUBLIC_URL, new Object[] { "" + id }));
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		} catch (final MalformedURLException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	List<String> getCompilerErrors(final long id) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			final Object[] result = (Object[])xmlRpcClient.execute(METHOD_JOB_COMPILER_ERRORS, new Object[] { "" + id });
			final List<String> l = new ArrayList<String>();
			for (final Object o : result)
				l.add((String)o);
			return l;
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	String getSource(final long id) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			return (String)xmlRpcClient.execute(METHOD_JOB_SOURCE, new Object[] { "" + id });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	String getOutput(final long id) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			return (String)xmlRpcClient.execute(METHOD_JOB_OUTPUT, new Object[] { "" + id });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	String getOutput(final long id, final long start, final long len) throws BoaException, NotLoggedInException {
		ensureLoggedIn();

		try {
			return (String)xmlRpcClient.execute(METHOD_JOB_PAGED_OUTPUT, new Object[] { "" + id, "" + start, "" + len });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}
}
