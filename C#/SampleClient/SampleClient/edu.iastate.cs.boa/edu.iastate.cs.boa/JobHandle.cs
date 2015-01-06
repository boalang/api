using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace edu.iastate.cs.boa
{
    public class JobHandle
    {
        private BoaClient client;
        private int id;
        private DateTime date;
        private InputHandle dataset;
        private CompileStatus compilerStatus;
        private ExecutionStatus execStatus;

        public JobHandle(BoaClient cli, int ID,DateTime dateTime, InputHandle data, CompileStatus compStat, ExecutionStatus execStat)
        {
            client = cli;
            id = ID;
            date = dateTime;
            dataset = data;
            compilerStatus = compStat;
            execStatus = execStat;
        }

        #region getterFunctions
        public BoaClient getClient()
        {
            return client;
        }

        public int getId()
        {
            return id;
        }

        public DateTime getDate()
        {
            return date;
        }

        public InputHandle getDataset()
        {
            return dataset;
        }

        public CompileStatus getCompilerStatus()
        {
            return compilerStatus;
        }

        public ExecutionStatus getExecutionStatus()
        {
            return execStatus;
        }
        #endregion

        public override string ToString()
        {
            return id + " (" + date + ") - " + dataset + " - compiler_status(" + compilerStatus + ") execution_status(" + execStatus + ")";
        }

        #region serverCalls
        /**
	     * Stops the job, if it is running.
	     *
	     * @throws BoaException if the command fails for any reason
	     * @throws NotLoggedInException if not already logged in to the API
	     */
	    public void stop() 
        {
		    client.stop(id);
	    }

        /**
	     * Resubmits the job.
	     *
	     * @throws BoaException if the command fails for any reason
	     * @throws NotLoggedInException if not already logged in to the API
	     */
        public void resubmit()
        {
            client.resubmit(id);
        }

        /**
	     * Deletes the job.
	     *
	     * @throws BoaException if the command fails for any reason
	     * @throws NotLoggedInException if not already logged in to the API
	     */
        public void delete()
        {
            client.delete(id);
        }

        /**
	     * Marks a job as public/private.
	     *
	     * @param isPublic should the job be public (<code>true</code>) or private (<code>false</code>)
	     * @throws BoaException if the command fails for any reason
	     * @throws NotLoggedInException if not already logged in to the API
	     */
        public void setPublic(bool isPublic)
        {
            client.setPublic(id, isPublic);
        }

        /**
	     * Get the job's public/private status.
	     *
	     * @return <code>true</code> if the job is public, else <code>false</code>
	     * @throws BoaException if the command fails for any reason
	     * @throws NotLoggedInException if not already logged in to the API
	     */
        public bool getPublic()
        {
            return client.getPublic(id);
        }

        /**
	     * Get the job's URL.
	     *
	     * @return a {@link java.net.URL} to view the job
	     * @throws BoaException if the command fails for any reason
	     * @throws NotLoggedInException if not already logged in to the API
	     */
	    public Uri getUrl() 
        {
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
	    public Uri getPublicUrl()
        {
		    return client.getPublicUrl(id);
	    }

	    /**
	     * Return any errors from trying to compile the job.
	     *
	     * @return a (possibly empty) {@link java.util.List} of compiler error messages
	     * @throws BoaException if the command fails for any reason
	     * @throws NotLoggedInException if not already logged in to the API
	     */
	    public List<String> getCompilerErrors() 
        {
		    return client.getCompilerErrors(id);
	    }

	    /**
	     * Return the source query for this job.
	     *
	     * @return the source query for this job
	     * @throws BoaException if the command fails for any reason
	     * @throws NotLoggedInException if not already logged in to the API
	     */
	    public String getSource() 
        {
		    return client.getSource(id);
	    }

	    /**
	     * Return the output for this job, if it finished successfully and has output.
	     *
	     * @return the output for this job
	     * @throws BoaException if the command fails for any reason
	     * @throws NotLoggedInException if not already logged in to the API
	     */
	    public String getOutput() 
        {
		    return client.getOutput(id);
	    }

	    /**
	     * Refreshes the cached data for this job.
	     *
	     * @throws BoaException if the command fails for any reason
	     * @throws NotLoggedInException if not already logged in to the API
	     */
	    public void refresh() 
        {
		    JobHandle j = client.getJob(id);

		    this.date = j.getDate();
		    this.compilerStatus = j.getCompilerStatus();
		    this.execStatus = j.getExecutionStatus();
	    }

        #endregion
    }

    public enum CompileStatus { WAITING, RUNNING, FINISHED, ERROR}
    public enum ExecutionStatus
    {
        WAITING,
        RUNNING,
        FINISHED,
        ERROR
    }
}
