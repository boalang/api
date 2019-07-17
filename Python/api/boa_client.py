import xmlrpc.client
import exceptions
import util

BOA_PROXY = "http://boa.cs.iastate.edu/boa/?q=boa/api"

class BoaClient(object):
    """ A client class for accessing boa's api 

    Attributes:
        server (:obj: `ServerProxy`):
        trans (:obj: `Transport`) 
    """

    def __init__(self):
        """Create a new Boa API client, using the standard domain/path."""
        self.trans = util.CookiesTransport()
        self.__logged_in = False
        self.server = xmlrpc.client.ServerProxy(BOA_PROXY, transport=self.trans)

    def login(self, username, password):
        """log into the boa framework using the remote api

        Args:
            username: username for boa account
            password: password for boa account
        """
        self.__logged_in = True
        response = self.server.user.login(username, password)
        self.trans.add_csrf(response["token"])
        return response

    def close(self):
        """Log out of the boa framework using the remote api"""
        self.ensure_logged_in()
        self.server.user.logout()
        self.__logged_in = False

    def ensure_logged_in(self):
        """Checks if a user is currently logged in through the remote api
        
        Raises:
            NotLoggedInException: if user is not currently logged in
        """
        if not self.__logged_in:
            raise NotLoggedInException("User not currently logged in")

    def datasets(self):
        """ Retrieves datasetsets currently provided by boa

        Returns:
            a list of boa datasets
        """
        self.ensure_logged_in()
        return self.server.boa.datasets()