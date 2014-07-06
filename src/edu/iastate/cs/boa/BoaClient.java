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
 *	 System.out.println(d);
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

	protected static final String METHOD_USER_LOGIN  = "user.login";
	protected static final String METHOD_USER_LOGOUT = "user.logout";

	protected static final String METHOD_BOA_DATASETS = "boa.datasets";
	protected static final String METHOD_BOA_JOBS     = "boa.jobs";
	protected static final String METHOD_BOA_SUBMIT   = "boa.submit";

	protected static final String METHOD_BOA_JOB_STOP            = "boa.job.stop";
	protected static final String METHOD_BOA_JOB_RESUBMIT        = "boa.job.resubmit";
	protected static final String METHOD_BOA_JOB_DELETE          = "boa.job.delete";
	protected static final String METHOD_BOA_JOB_SET_PUBLIC      = "boa.job.setpublic";
	protected static final String METHOD_BOA_JOB_PUBLIC          = "boa.job.public";
	protected static final String METHOD_BOA_JOB_URL             = "boa.job.url";
	protected static final String METHOD_BOA_JOB_PUBLIC_URL      = "boa.job.publicurl";
	protected static final String METHOD_BOA_JOB_COMPILER_ERRORS = "boa.job.compilerErrors";
	protected static final String METHOD_BOA_JOB_SOURCE          = "boa.job.source";

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

			loggedIn = true;
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
		try {
			xmlRpcClient.execute(METHOD_USER_LOGOUT, new Object[] {});
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	/**
	 * Returns a list of available input datasets.
	 *
	 * @return a {@link java.util.Map} where keys are dataset IDs and values are their names
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	@SuppressWarnings("unchecked")
	public List<InputHandle> getDatasets() throws BoaException, NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();

		try {
			final Object[] result = (Object[]) xmlRpcClient.execute(METHOD_BOA_DATASETS, new Object[] {});

			final List<InputHandle> datasets = new ArrayList<InputHandle>();
			for (int i = 0; i < result.length; i++)
				datasets.add(Util.parseDataset((Map<String, Object>)result[i]));

			return datasets;
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
		if (!loggedIn)
			throw new NotLoggedInException();

		final List<JobHandle> jobs = getJobList();
		if (jobs.isEmpty())
			return null;

		return jobs.get(0);
	}

	/**
	 * Returns a list of the most recent jobs.
	 *
	 * @return a {@link JobHandle} for the latest job, or <code>null</code> if no jobs exist
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
	@SuppressWarnings("unchecked")
	public List<JobHandle> getJobList() throws BoaException, NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();

		try {
			final Object[] result = (Object[])xmlRpcClient.execute(METHOD_BOA_JOBS, new Object[] {});

			final List<JobHandle> jobs = new ArrayList<JobHandle>();
			for (int i = 0; i < result.length; i++)
				jobs.add(Util.parseJob(this, (Map<String, Object>)result[i]));

			return jobs;
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
	@SuppressWarnings("unchecked")
	public JobHandle query(final String query, final InputHandle dataset) throws BoaException, NotLoggedInException{
		if (!loggedIn)
			throw new NotLoggedInException();

		try {
			return Util.parseJob(this, (Map<String, Object>)xmlRpcClient.execute(METHOD_BOA_SUBMIT, new Object[] { query, dataset.getId() }));
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
	@SuppressWarnings("unchecked")
	public JobHandle testQuery(final String query) throws BoaException, NotLoggedInException{
		if (!loggedIn)
			throw new NotLoggedInException();

		try {
			return Util.parseJob(this, (Map<String, Object>)xmlRpcClient.execute(METHOD_BOA_SUBMIT, new Object[] { query, getDatasets().get(0).getId() }));
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// the methods below are not meant to be called by clients directly //
	// but rather through a handle                                      //
	//////////////////////////////////////////////////////////////////////

	void stop(final long id) throws BoaException, NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();

		try {
			xmlRpcClient.execute(METHOD_BOA_JOB_STOP, new Object[] { "" + id });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
	}

	void resubmit(final long id) throws BoaException, NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();

		try {
			xmlRpcClient.execute(METHOD_BOA_JOB_RESUBMIT, new Object[] { "" + id });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}

		throw new BoaException("The resubmit() method is not yet implemented.");
	}

	void delete(final long id) throws BoaException, NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();

		/* TODO - implement on server side
		try {
			xmlRpcClient.execute(METHOD_BOA_JOB_DELETE, new Object[] { "" + id });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
		*/

		throw new BoaException("The delete() method is not yet implemented.");
	}

	void setPublic(final long id, final boolean isPublic) throws BoaException, NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();

		/* TODO - implement on server side
		try {
			xmlRpcClient.execute(METHOD_BOA_JOB_SET_PUBLIC, new Object[] { "" + id, isPublic });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
		*/

		throw new BoaException("The setPublic() method is not yet implemented.");
	}

	boolean getPublic(final long id) throws BoaException, NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();

		/* TODO - implement on server side
		try {
			return xmlRpcClient.execute(METHOD_BOA_JOB_PUBLIC, new Object[] { "" + id });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
		*/

		throw new BoaException("The getPublic() method is not yet implemented.");
	}

	URL getUrl(final long id) throws BoaException, NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();

		/* TODO - implement on server side
		try {
			return xmlRpcClient.execute(METHOD_BOA_JOB_URL, new Object[] { "" + id });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
		*/

		throw new BoaException("The getUrl() method is not yet implemented.");
	}

	URL getPublicUrl(final long id) throws BoaException, NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();

		/* TODO - implement on server side
		try {
			return xmlRpcClient.execute(METHOD_BOA_JOB_PUBLIC_URL, new Object[] { "" + id });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
		*/

		throw new BoaException("The getPublicUrl() method is not yet implemented.");
	}

	List<String> getCompilerErrors(final long id) throws BoaException, NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();

		/* TODO - implement on server side
		try {
			return xmlRpcClient.execute(METHOD_BOA_JOB_COMPILER_ERRORS, new Object[] { "" + id });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
		*/

		throw new BoaException("The getCompilerErrors() method is not yet implemented.");
	}

	String getSource(final long id) throws BoaException, NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();

		/* TODO - implement on server side
		try {
			return xmlRpcClient.execute(METHOD_BOA_JOB_SOURCE, new Object[] { "" + id });
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
		*/

		throw new BoaException("The getSource() method is not yet implemented.");
	}
}
