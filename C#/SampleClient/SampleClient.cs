using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

using edu.iastate.cs.boa;

namespace SampleClient
{
    class SampleClient
    {
        static void Main(string[] args)
        {
            try
            {
                if (args.Length != 2)
                {
                    Console.Error.WriteLine("Error: wrong number of arguments");
                    Console.Error.WriteLine("Use: SampleClient <username> <password>");
                    Environment.Exit(-1);
                }

                // create a client and log into the remote server
                BoaClient client = new BoaClient();

                client.login(args[0], args[1]);
                Console.WriteLine("Logged In");

                // dump the list of available input datasets
                client.getDatasets().ForEach(Print);

                Console.WriteLine("number of jobs: " + client.getJobCount());
                Console.WriteLine("number of public jobs: " + client.getJobCount(true));

                // show the oldest 10 jobs
                client.getJobList(Math.Max(10, client.getJobCount()) - 10, 10).ForEach(PrintJ);

                JobHandle lastJob = client.getLastJob();
                Console.WriteLine("Last job: " + lastJob);
		        Console.WriteLine("URL: " + lastJob.getUrl());
		        Console.WriteLine("Public URL: " + lastJob.getPublicUrl());
		        Console.WriteLine("Public? " + lastJob.getPublic());
		        lastJob.setPublic(!lastJob.getPublic());
		        Console.WriteLine("Public? " + lastJob.getPublic());
		        lastJob.setPublic(!lastJob.getPublic());
		        Console.WriteLine("Source:");
		        Console.WriteLine("---------------------");
		        Console.WriteLine(lastJob.getSource());
		        Console.WriteLine("---------------------");
                lastJob.getCompilerErrors().ForEach(PrintS);
		        Console.WriteLine("Output:");
		        Console.WriteLine("---------------------");
		        Console.WriteLine(lastJob.getOutput());
		        Console.WriteLine("---------------------");

                // create a new job by submitting a query and then do things with it
                JobHandle j = client.query("o: output sum of int;\no << 1;");
                Console.WriteLine("Submitted: " + j);

                j.stop();
		        Console.WriteLine("Stopped job: " + j);

		        j.delete();
                Console.WriteLine("Deleted job: " + j);

                // when finished, close the connection and log out of the remote server
                client.close();
                Console.WriteLine("logged out");

                Console.Read();
            }
            catch(BoaException e)
            {
                Console.Error.WriteLine(e.Message);
            }
            finally
            {
                Console.Read();
            }
        }

        private static void Print(InputHandle s)
        {
            Console.WriteLine(s);
        }

        private static void PrintJ(JobHandle s)
        {
            Console.WriteLine(s);
        }

        private static void PrintS(String s)
        {
            Console.WriteLine("Compile error: "+ s);
        }
    }
}
