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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.*;

/**
 * A client class for accessing Boa's API.
 *
 * @author rdyer
 */
public class BoaClient {
	private static final String BOA_DOMAIN = "boa.cs.iastate.edu";
	private static final String BOA_PATH = "/boa/?q=boa/api";

	protected static final String METHOD_USER_LOGIN = "user.login";
    protected static final String METHOD_USER_LOGOUT = "user.logout";

    protected static final String METHOD_BOA_DATASETS = "boa.datasets";
    protected static final String METHOD_BOA_JOBS = "boa.jobs";

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
	 * @throws NotLoggedInException if not already logged in to the API
	 */
    public void logout() throws BoaException, NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();

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

		final List<JobHandle> jobs = jobList();
		if (jobs.isEmpty())
			return null;

		return jobs.get(0);
	}

	/**
	 * Returns the most recent job.
	 *
	 * @return a {@link JobHandle} for the latest job, or <code>null</code> if no jobs exist
	 * @throws BoaException if there was a problem reading from the server
	 * @throws NotLoggedInException if not already logged in to the API
	 */
    public List<JobHandle> jobList() throws BoaException, NotLoggedInException {
		if (!loggedIn)
			throw new NotLoggedInException();

		try {
			final Object[] result = (Object[])xmlRpcClient.execute(METHOD_BOA_JOBS, new Object[] {});

			final List<JobHandle> jobs = new ArrayList<JobHandle>();
			for (int i = 0; i < result.length; i++)
				jobs.add(Util.parseJob((Map<String, Object>)result[i]));

			return jobs;
		} catch (final XmlRpcException e) {
			throw new BoaException(e.getMessage(), e);
		}
    }

	/**
	 * An example Boa client.  Takes the username and password as first 2 arguments
	 * on commandline, then does some simple tasks to show the API works.
	 */
    public static void main(final String[] args) throws Exception {
    	final BoaClient client = new BoaClient();

		if (args.length != 2) {
			System.err.println("Error: expected username and password as argument");
			System.exit(-1);
		}

        client.login(args[0], args[1]);
        System.out.println("logged in");

		final List<InputHandle> datasets = client.getDatasets();
		for (final InputHandle d : datasets)
			System.out.println(d);

		System.out.println("Last job submitted: " + client.getLastJob());

		//int id = client.run("....", 3);

        client.logout();
        System.out.println("logged out");
    }
}
