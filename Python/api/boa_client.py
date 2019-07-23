import xmlrpc.client
import exceptions
import util

BOA_PROXY = "http://boa.cs.iastate.edu/boa/?q=boa/api"

class BoaClient(object):
    """ A client class for accessing boa's api 

    Attributes:
        server (xmlrpc.client.ServerProxy):
        trans (xmlrpc.client.Transport)
    """

    def __init__(self):
        """Create a new Boa API client, using the standard domain/path."""
        self.trans = util.CookiesTransport()
        self.__logged_in = False
        self.server = xmlrpc.client.ServerProxy(BOA_PROXY, transport=self.trans)

    def login(self, username, password):
        """log into the boa framework using the remote api

        Args:
            username (str): username for boa account
            password (str): password for boa account
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
        
        Returns:
            bool: True if user is logged in, false if otherwise
        
        Raises:
            NotLoggedInException: if user is not currently logged in
        """
        if not self.__logged_in:
            raise NotLoggedInException("User not currently logged in")

    def datasets(self):
        """ Retrieves datasetsets currently provided by boa

        Returns:
            list: a list of boa datasets
        """
        self.ensure_logged_in()
        return self.server.boa.datasets()

    def dataset_names(self):
        """Retrieves a list of names of all datasets provided by boa
        
        Returns:
            list: the dataset names
        """
        self.ensure_logged_in()
        dataset_names = []
        datasets = self.datasets()
        for x in datasets:
            dataset_names.append(x['name'])
        return dataset_names

    def get_dataset(self, name):
        """Retrieves a dataset given a name.
        
        Args:
            name (str): The name of the input dataset to return.

        Returns:
            dict: a dictionary with the keys id and name
        """
        self.ensure_logged_in()
        for x in self.datasets():
            if x['name'] == name:
                return x
        return None

    def last_job(self):
        """Retrieves the most recently submitted job

        Returns:
            JobHandle: the last submitted job
        """
        self.ensure_logged_in()
        jobs = self.job_list(False, 0, 1)
        return jobs[0]

    def job_count(self, pub_only=False):
        """Retrieves the number of jobs submitted by a user
        
        Args:
            pub_only (bool, optional): if true, return only public jobs
                otherwise return all jobs
        
        Returns:
            int: the number of jobs submitted by a user
        """
        self.ensure_logged_in()
        return self.server.boa.count(pub_only)

    def query(self, query, dataset=None):
        """Submits a new query to Boa to query the specified and returns a handle to the new job.

        Args:
            query (str): a boa query represented as a string.
            dataset (str, optional): the name of the input dataset.

        Returns: 
            (JobHandle) a job 
        """
        self.ensure_logged_in()
        id = 0 if dataset is None else dataset.get_id()
        job = self.server.boa.submit(query, self.get_datasets()[id]['id'])
        return parse_job(self, job)

    def get_job(self, id):
        """Retrieves a job given an id.
        
        Args:
            id (int): the id of the job you want to retrieve 

        Returns:
            JobHandle: the desired job.
        """
        self.ensure_logged_in()
        return parse_job(self.server, self.server.boa.job(id))

    def job_list(self, pub_only=False, offset=0, length=1000):
        """Returns a list of the most recent jobs, based on an offset and length.
        
        This includes public and private jobs.  Returned jobs are ordered from newest to oldest

        Args:
            pub_only (bool, optional): if true, only return public jobs otherwise return all jobs
            offset  (int, optional): the starting offset
            length (int, optional): the number of jobs (at most) to return

        Returns:
            list: a list of jobs where each element is a jobHandle
        """
        self.ensure_logged_in()
        list = self.server.boa.jobs(pub_only, offset, length)
        newDict = []
        if(len(list) > 0):
            for i in list:
                newDict.append(util.parse_job(self, i))
        return newDict

    def stop(self, job):
        self.ensure_logged_in()
        self.server.job.stop(job.id)

    def resubmit(self, job):
        self.ensure_logged_in()
        self.server.job.resubmit(job.id)

    def delete(self, job):
        self.ensure_logged_in()
        self.server.job.delete(job.id)

    def set_public(self, job, is_public):
        self.ensure_logged_in()
        if is_public is True:
            self.server.job.setpublic(job.id, 1)
        else:
            self.server.job.setpublic(job.id, 0)

    def get_public(self, job):
        self.ensure_logged_in()
        result = self.server.job.public(job.id)
        if result is 1:
            return True
        else:
            return False

    def get_url(self, job):
        self.ensure_logged_in()
        return self.server.job.url(job.id)

    def get_public_url(self, job):
        self.ensure_logged_in()
        return self.server.job.publicurl(job.id)

    def get_compiler_errors(self, job):
        self.ensure_logged_in()
        return self.server.job.compilerErrors(job.id)

    def get_source(self, job):
        self.ensure_logged_in()
        return self.server.job.source(job)

    def get_output(self, job, start, length):
        self.ensure_logged_in()
        return self.server.job.output(job.id, start, length)