import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.*;

public class BoaClient {
	private static final String BOA_DOMAIN = "boa.cs.iastate.edu";
	private static final String BOA_PATH = "/boa/?q=boa/api";

	private static final String METHOD_USER_LOGIN = "user.login";
    private static final String METHOD_USER_LOGOUT = "user.logout";

    private static final String METHOD_BOA_DATASETS = "boa.datasets";

    private final XmlRpcClient xmlRpcClient;

    public BoaClient() {
    	this(BOA_DOMAIN, BOA_PATH);
    }

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

        xmlRpcClient = new XmlRpcClient();
        xmlRpcClient.setConfig(config);
    }

    public Map<String, String> login(final String username, final String password) throws LoginException {
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

			return response;
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

    public void logout() throws XmlRpcException {
        xmlRpcClient.execute(METHOD_USER_LOGOUT, new Object[] {});
    }

    public Map<String, String> getDatasets() throws XmlRpcException {
        return (Map<String, String>) xmlRpcClient.execute(METHOD_BOA_DATASETS, new Object[] {});
    }

	private String getError() {
		return null;
	}

    public static void main(final String[] args) throws Exception {
    	final BoaClient service = new BoaClient();

		if (args.length != 2) {
			System.err.println("Error: expected username and password as argument");
			System.exit(-1);
		}

        service.login(args[0], args[1]);
        System.out.println("logged in");

		final Map<String, String> datasets = service.getDatasets();
		final List<String> keys = new ArrayList<String>();
		keys.addAll(datasets.keySet());
		Collections.sort(keys);
		for (final String k : keys)
			System.out.println(k + " - " + datasets.get(k));

        service.logout();
        System.out.println("logged out");
    }
}
