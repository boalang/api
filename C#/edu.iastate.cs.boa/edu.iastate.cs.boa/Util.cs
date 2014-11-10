using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using CookComputing.XmlRpc;
using System.Globalization;

namespace edu.iastate.cs.boa
{
    public class Util
    {
        public static JobHandle parseJob(BoaClient client, XmlRpcStruct job)
        {
            String[] keys = {"id", "submitted", "input", "compiler_status", "hadoop_status"};
            verifyKeys(job, keys);
            String date = ((String)job["submitted"]).Replace("-", "/");
            DateTime newDate = new DateTime(Convert.ToInt32(date.Substring(0, 4)), Convert.ToInt32(date.Substring(5, 2)), Convert.ToInt32(date.Substring(8, 2)), Convert.ToInt32(date.Substring(11, 2))
                , Convert.ToInt32(date.Substring(14, 2)), Convert.ToInt32(date.Substring(17, 2)));
            return new JobHandle(
                client, 
                Convert.ToInt32((String)job["id"]),
                newDate,
                parseDataset((XmlRpcStruct)job["input"]), 
                strToCompileStatus((String)job["compiler_status"]),
                strToExecutionStatus((String)job["hadoop_status"])
                );
        }

        public static InputHandle parseDataset(XmlRpcStruct input)
        {
            String[] keys = { "id", "name" };
            verifyKeys(input, keys);
            return new InputHandle(Convert.ToInt32((String)input["id"]), (String)input["name"]);
        }

        private static void verifyKeys(XmlRpcStruct m, String[] keys)
        {
            foreach(String key in keys)
            {
                if(m.ContainsKey(key) == false)
                {
                    throw new BoaException("Invalid response from server: response does not contain key '" + key + "'.");
                }

            }
        }

        private static CompileStatus strToCompileStatus(String s)
        {
		    if ("Error" == s )
			    return CompileStatus.ERROR;
            if ("Finished" == s)
			    return CompileStatus.FINISHED;
            if ("Running" == s)
			    return CompileStatus.RUNNING;
            if ("Waiting" == s)
			    return CompileStatus.WAITING;
		    throw new BoaException("Invalid response from server: compile_status '" + s + "' unknown");
	    }
        private static ExecutionStatus strToExecutionStatus(String s) 
        {
            if ("Error" == s)
			    return ExecutionStatus.ERROR;
            if ("Finished" == s)
			    return ExecutionStatus.FINISHED;
            if ("Running" == s)
			    return ExecutionStatus.RUNNING;
            if ("Waiting" == s)
			    return ExecutionStatus.WAITING;
		    throw new BoaException("Invalid response from server: execution_status '" + s + "' unknown");
	    }
    }
}
