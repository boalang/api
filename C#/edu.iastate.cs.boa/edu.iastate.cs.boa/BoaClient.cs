using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

using System.Runtime.Remoting;
using System.Runtime.Remoting.Channels;
using System.Runtime.Remoting.Channels.Http;
using CookComputing.XmlRpc;

using System.Net;
using System.IO;

namespace edu.iastate.cs.boa
{
    public struct BoaLogin
    {
        public string session_name;
        public string sessid;
        public string token;
    }
    public struct BoaConnect
    {
        public string session_name;
        public string sessid;
    }
    public struct BoaToken
    {
        public string token;
    }
    
    [XmlRpcUrl("http://boa.cs.iastate.edu/boa/?q=boa/api")]
    public interface Boa : IXmlRpcProxy
    {
        [XmlRpcMethod("user.login")]
        BoaLogin loginRequest(string uname, string pword);
        [XmlRpcMethod("system.connect")]
        BoaConnect connectRequest(string uname, string pword);
        [XmlRpcMethod("user.token")]
        BoaToken tokenRequest();
        [XmlRpcMethod("user.logout")]
        bool logoutRequest();
        [XmlRpcMethod("boa.datasets")]
        Object[] datasetRquest();
        [XmlRpcMethod("boa.job")]
        XmlRpcStruct jobRequest(int id);
        [XmlRpcMethod("boa.jobs")]
        Object[] jobsRequest(bool flag);
        [XmlRpcMethod("boa.range")]
        Object[] jobsRangeRequest(bool pubOnly, int offset, int length);
        [XmlRpcMethod("boa.count")]
        String countRequest(bool pubOnly);
        [XmlRpcMethod("boa.submit")]
        XmlRpcStruct submitRequest(String query, int id);
        [XmlRpcMethod("job.stop")]
        void stopRequest(String id);
        [XmlRpcMethod("job.resubmit")]
        void resubmitRequest(String id);
        [XmlRpcMethod("job.delete")]
        void deteleteRequest(String id);
        [XmlRpcMethod("job.setpublic")]
        void setpublicRequest(String id, bool isPublic);
        [XmlRpcMethod("job.public")]
        int ispublicRequest(String id);
        [XmlRpcMethod("job.url")]
        String urlRequest(String id);
        [XmlRpcMethod("job.publicurl")]
        String publicurlRequest(String id);
        [XmlRpcMethod("job.compilerErrors")]
        Object[] compilererrorRequest(String id);
        [XmlRpcMethod("job.source")]
        String sourceRequest(String id);
        [XmlRpcMethod("job.output")]
        String outputRequest(String id);
    }



	public class BoaClient : IDisposable
    {
        private static readonly String BOA_DOMAIN = "boa.cs.iastate.edu";
	    private static readonly String BOA_PATH   = "/boa/?q=boa/api";

        //protected static readonly String METHOD_SYSTEM_CONNECT = "system.connect";

	    //protected static readonly String METHOD_USER_LOGIN  = "user.login";
	    //protected static readonly String METHOD_USER_LOGOUT = "user.logout";

	    //protected static readonly String METHOD_BOA_DATASETS   = "boa.datasets";
	    //protected static readonly String METHOD_BOA_JOB        = "boa.job";
	    //protected static readonly String METHOD_BOA_JOBS       = "boa.jobs";
	    //protected static readonly String METHOD_BOA_JOBS_COUNT = "boa.count";
	    //protected static readonly String METHOD_BOA_JOBS_RANGE = "boa.range";
	    //protected static readonly String METHOD_BOA_SUBMIT     = "boa.submit";

	    //protected static readonly String METHOD_JOB_STOP            = "job.stop";
	    //protected static readonly String METHOD_JOB_RESUBMIT        = "job.resubmit";
	    //protected static readonly String METHOD_JOB_DELETE          = "job.delete";
	    //protected static readonly String METHOD_JOB_SET_PUBLIC      = "job.setpublic";
	    //protected static readonly String METHOD_JOB_PUBLIC          = "job.public";
	    //protected static readonly String METHOD_JOB_URL             = "job.url";
	    //protected static readonly String METHOD_JOB_PUBLIC_URL      = "job.publicurl";
	    //protected static readonly String METHOD_JOB_COMPILER_ERRORS = "job.compilerErrors";
	    //protected static readonly String METHOD_JOB_SOURCE          = "job.source";
	    //protected static readonly String METHOD_JOB_OUTPUT          = "job.output";

        protected Boa xmlRpcClient = XmlRpcProxyGen.Create<Boa>();

	    protected bool loggedIn = false;

        public BoaClient()
        {
            String domain = BOA_DOMAIN;
            String path = BOA_PATH;
            /*
            * Endpoint is defined within Drupal services module configuration in order to define
            * a URL that is available for serving a specific set of service calls. See drupal
            * documentation for "Services 3.X". <a href="http://drupal.org/node/783236">http://drupal.org/node/783236</a>
            */
            xmlRpcClient.Url = "http://" + domain + path;
        }

		~BoaClient () {
			try {
				close();
			} catch (Exception) {
				// ignore
			}
		}

        public BoaClient(String domain, String path)
        {
            if (domain.IndexOf("/") != -1)
                throw new ArgumentException("Argument 'domain' should not contain the protocol (http://) or a path (/).");
            if (path.IndexOf("/") != 0)
                throw new ArgumentException("Argument 'path' should start with '/'.");

            /*
            * Endpoint is defined within Drupal services module configuration in order to define
            * a URL that is available for serving a specific set of service calls. See drupal
            * documentation for "Services 3.X". <a href="http://drupal.org/node/783236">http://drupal.org/node/783236</a>
            */
            xmlRpcClient.Url = "http://" + domain + path;


            /*XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		    try {
			    config.setServerURL(new URL(endpointURL));
		    } catch (final MalformedURLException e) {
			    // only happens if no/invalid protocol given, but we ensure this never happens
		    }

		    xmlRpcClient.setConfig(config);*/
        }

        public void login(String username, String password)
        {
            if (loggedIn)
                return;
            try
            {
                BoaLogin ret = xmlRpcClient.loginRequest(username, password);
                String cook = ret.session_name + "=" + ret.sessid;
                xmlRpcClient.Headers.Add("Cookies", cook);
                xmlRpcClient.Headers.Add("X-CSRF-Token", ret.token);
            }
            catch (XmlRpcFaultException e)
            {
                if(e.Message.IndexOf("Already logged in as ")==0)
                {
                    Console.Error.WriteLine("Connect");
                    connect(username, password);
                }
                
                else
                {
                    if (e.Message.IndexOf("username") != -1)
                        throw new LoginException("Invalid username or password.", e);
                    if (e.Message.IndexOf("response") != -1)
                        throw new LoginException("Invalid domain given to Boa API.", e);
                    if (e.Message.IndexOf(":") != -1)
                        throw new LoginException(e.Message.Substring(e.Message.IndexOf(":") + 2), e);
                    throw new LoginException(e.Message, e);
                }
            }
            loggedIn = true;
        }

        public void connect(String username, String password)
        {
            try
            {
                BoaConnect ret = xmlRpcClient.connectRequest(username, password);
                BoaToken ret2 = xmlRpcClient.tokenRequest();
                String cook = ret.session_name + "=" + ret.sessid;
                xmlRpcClient.Headers.Add("Cookies", cook);
                xmlRpcClient.Headers.Add("X-CSRF-Token", ret2.token);
            }
            catch (XmlRpcFaultException e)
            {
                if (e.Message.IndexOf("username") != -1)
                    throw new LoginException("Invalid username or password.", e);
                if (e.Message.IndexOf("response") != -1)
                    throw new LoginException("Invalid domain given to Boa API.", e);
                if (e.Message.IndexOf(":") != -1)
                    throw new LoginException(e.Message.Substring(e.Message.IndexOf(":") + 2), e);
                throw new LoginException(e.Message, e);
                
            }
        }

		private bool disposed = false;

		public void Dispose() {
			if (!disposed) {
				disposed = true;
				close ();
				GC.SuppressFinalize(this);
			}
		}

		public void close()
        {
            resetDataSetCache();
            try
            {
				loggedIn = false;
                xmlRpcClient.logoutRequest();
            }
            catch(XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
            }
        }

        /**
	     * Checks if the API is logged in and if not, throws an
	     * exception.
	     *
	     * @throws NotLoggedInException if the API is not logged in
	     */
        public void ensureLoogedIn()
        {
            if (!loggedIn)
            {
                throw new NotLoggedInException();
            }
        }

        protected List<InputHandle> datasetCache = null;
        protected long datasetCacheTime = 0;


        /**
	     * Resets the internal dataset cache.
	     */
        public void resetDataSetCache()
        {
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
        public List<InputHandle> getDatasets()
        {
            DateTime Jan1st1970 = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            if (datasetCache != null && datasetCacheTime + 86400000 > (DateTime.UtcNow - Jan1st1970).TotalMilliseconds)
            {
                return datasetCache;
            }

            ensureLoogedIn();

            try
            {
                Object[] res = xmlRpcClient.datasetRquest();

                datasetCache = new List<InputHandle>();
                for(int i = 0; i< res.Length; i++)
                {
                    datasetCache.Add(Util.parseDataset((XmlRpcStruct)res[i]));
                }
                datasetCacheTime = (long)(DateTime.UtcNow - Jan1st1970).TotalMilliseconds;

                return datasetCache;
            }
            catch(XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
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
        public String[] getDatasetNames()
        {
            List<InputHandle> list = getDatasets();
            String[] items = new String[list.Count];

            for(int i = 0; i < list.Count;i++)
            {
                items[i] = list[i].getName();
            }

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
        public InputHandle getDataset(String name)
        {
            foreach(InputHandle h in getDatasets())
            {
                if(h.getName() == name)
                {
                    return h;
                }
            }

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
        public JobHandle getJob(int id)
        {
            ensureLoogedIn();
            try
            {
                return Util.parseJob(this, xmlRpcClient.jobRequest(id));
            }
            catch(XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
            }
        }

        /**
	     * Returns the most recent job.
	     *
	     * @return a {@link JobHandle} for the latest job, or <code>null</code> if no jobs exist
	     * @throws BoaException if there was a problem reading from the server
	     * @throws NotLoggedInException if not already logged in to the API
	     */
        public JobHandle getLastJob()
        {
            ensureLoogedIn();

            List<JobHandle> jobs = getJobList(0, 1);
            if (jobs.Count == 0)
                return null;

            return jobs[0];
        }

        /**
	     * Returns a list of the most recent jobs.  The number of jobs is limited based on the user's web setting.
	     * This includes public and private jobs.  Returned jobs are ordered from newest to oldest.
	     *
	     * @return a list of {@link JobHandle}s for the most recent jobs
	     * @throws BoaException if there was a problem reading from the server
	     * @throws NotLoggedInException if not already logged in to the API
	     */
	    public List<JobHandle> getJobList() 
        {
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
        public List<JobHandle> getJobList(int offset, int length)
        {
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
        public List<JobHandle> getJobList(Boolean pubOnly)
        {
            ensureLoogedIn();

            try
            {
                Object[] res = xmlRpcClient.jobsRequest(pubOnly);
                List<JobHandle> jobs = new List<JobHandle>();
                for (int i = 0; i < res.Length; i++)
                {
                    jobs.Add(Util.parseJob(this, (XmlRpcStruct)res[i]));
                }

                return jobs;
            }
            catch(XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
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
        public List<JobHandle> getJobList(bool pubOnly, int offset, int length)
        {
            ensureLoogedIn();

            try
            {
                Object[] res = xmlRpcClient.jobsRangeRequest(pubOnly, offset, length);
                List<JobHandle> jobs = new List<JobHandle>();
                for (int i = 0; i < res.Length; i++)
                {
                    jobs.Add(Util.parseJob(this, (XmlRpcStruct)res[i]));
                }

                return jobs;
            }
            catch (XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
            }
        }

        /**
	     * Returns the number of jobs for the user.  This includes public and private jobs.
	     *
	     * @return the number of jobs the user has created
	     * @throws BoaException if there was a problem reading from the server
	     * @throws NotLoggedInException if not already logged in to the API
	     */
        public int getJobCount()
        {
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
	    public int getJobCount(bool pubOnly)
        {
            try
            {
                return Convert.ToInt32(xmlRpcClient.countRequest(pubOnly));
            }
            catch(XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
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
        public JobHandle query(String query, InputHandle dataset)
        {
            ensureLoogedIn();

            try
            {
                return Util.parseJob(this, xmlRpcClient.submitRequest(query, dataset.getId()));
            }
            catch(XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
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
        public JobHandle query(String query)
        {
            ensureLoogedIn();

            try
            {
                return Util.parseJob(this, xmlRpcClient.submitRequest(query, getDatasets()[0].getId()));
            }
            catch (XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
            }
        }


        //////////////////////////////////////////////////////////////////////
        // the methods below are not meant to be called by clients directly //
        // but rather through a handle                                      //
        //////////////////////////////////////////////////////////////////////

        public void stop(int id)
        {
            ensureLoogedIn();

            try
            {
                xmlRpcClient.stopRequest("" + id);
            }
            catch(XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
            }
        }

        public void resubmit(int id)
        {
            ensureLoogedIn();

            try
            {
                xmlRpcClient.resubmitRequest("" + id);
            }
            catch (XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
            }
        }

        public void delete(int id)
        {
            ensureLoogedIn();

            try
            {
                xmlRpcClient.deteleteRequest("" + id);
            }
            catch (XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
            }
        }

        public void setPublic(int id, bool isPublic)
        {
            ensureLoogedIn();

            try
            {
                xmlRpcClient.setpublicRequest("" + id, isPublic);
            }
            catch (XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
            }
        }

        public bool getPublic(int id)
        {
            ensureLoogedIn();

            try
            {
                if (xmlRpcClient.ispublicRequest("" + id) > 0)
                {
                    return true;
                }
                return false;
            }
            catch (XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
            }
        }

        public Uri getUrl(int id)
        {
            ensureLoogedIn();

            try 
            {
                return new Uri(xmlRpcClient.urlRequest("" + id));
		    } 
            catch (XmlRpcException e) 
            {
			    throw new BoaException(e.Message, e);
		    }
        }

        public Uri getPublicUrl(int id)
        {
            ensureLoogedIn();

            try
            {
                return new Uri(xmlRpcClient.publicurlRequest("" + id));
            }
            catch (XmlRpcException e)
            {
                throw new BoaException(e.Message, e);
            }
        }

        public List<String> getCompilerErrors(int id)
        {
            ensureLoogedIn();

            try
            {
                Object[] res = xmlRpcClient.compilererrorRequest("" + id);
                List<String> l = new List<String>();

                foreach(Object o in res)
                {
                    l.Add((String)o);
                }
                return l;
            }
            catch(XmlRpcFaultException e)
            {
                throw new BoaException(e.Message, e);
            }
        }

        public String getSource(int id)
        {
            ensureLoogedIn();

            try
            {
                return xmlRpcClient.sourceRequest("" + id);
            }
            catch (XmlRpcException e)
            {
                throw new BoaException(e.Message, e);
            }
        }

        public String getOutput(int id)
        {
            ensureLoogedIn();

            try
            {
                return xmlRpcClient.outputRequest("" + id);
            }
            catch (XmlRpcException e)
            {
                throw new BoaException(e.Message, e);
            }
        }
    }
}
