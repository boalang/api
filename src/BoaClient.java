import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.*;

public class BoaClient {
	private static final String BOA_DOMAIN = "boa.cs.iastate.edu";
	private static final String BOA_PATH = "/boa/?q=boa/api";

	private static final String METHOD_USER_LOGIN = "user.login";
    private static final String METHOD_USER_LOGOUT = "user.logout";

    /**
     * Endpoint is defined within Drupal services module configuration in order to define
     * a URL that is available for serving a specific set of service calls. See drupal
     * documentation for "Services 3.X". <a href="http://drupal.org/node/783236">http://drupal.org/node/783236</a>
     */
    private final String endpointURL;
    private final XmlRpcClient xmlRpcClient;

    public BoaClient() throws MalformedURLException {
    	this(BOA_DOMAIN, BOA_PATH);
    }

    public BoaClient(final String domain, final String path) throws MalformedURLException {
        this.endpointURL = "http://" + domain + path;
        final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(this.endpointURL));
        xmlRpcClient = new XmlRpcClient();
        xmlRpcClient.setConfig(config);
    }

    public Map<String, String> login(final String username, final String password) throws XmlRpcException {
    	final Vector<String> params = new Vector<String>();
        params.add(username);
        params.add(password);

        @SuppressWarnings("unchecked")
		final Map<String, String> response = (Map<String, String>) xmlRpcClient.execute(METHOD_USER_LOGIN, params);

        // construct a custom transport that sets the session cookie and CSRF token
        final String cookie = response.get("session_name") + "=" + response.get("sessid");
		final String token = (String)response.get("token");
        xmlRpcClient.setTransportFactory(new XmlRpcSunHttpTransportFactory(xmlRpcClient) {
            public XmlRpcTransport getTransport() {
                return new XmlRpcSunHttpTransport(xmlRpcClient) {
                    @Override
                    protected void initHttpHeaders(XmlRpcRequest request) throws XmlRpcClientException {
                        super.initHttpHeaders(request);
                        setRequestHeader("Cookie", cookie);
						setRequestHeader("X-CSRF-Token", token);
                    }
                };
            }
        });

        return response;
    }

    public void logout() throws XmlRpcException {
        xmlRpcClient.execute(METHOD_USER_LOGOUT, new Vector<Object>());
    }

    public static void main(final String[] args) throws Exception {
    	final BoaClient service = new BoaClient();
        service.login("demo", "boa demo password");
        System.out.println("logged in");
        service.logout();
        System.out.println("logged out");
    }
}
