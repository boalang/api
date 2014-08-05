/*
 * Copyright 2014, Robert Dyer,
 *                 and Bowling Green State University
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

/**
 * An example Boa client.  Takes the username and password as first 2 arguments
 * on commandline, then does some simple tasks to show the API works.
 *
 * @author rdyer
 */
public class SampleClient {
	public static void main(final String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Error: wrong number of arguments");
			System.err.println("Use: SampleClient <username> <password>");
			System.exit(-1);
		}


		// create a client and log into the remote server
		final BoaClient client = new BoaClient();

		client.login(args[0], args[1]);
		System.out.println("logged in");


		// dump the list of available input datasets
		for (final InputHandle d : client.getDatasets())
			System.out.println(d);


		// how many jobs do they have
		System.out.println("number of jobs: " + client.getJobCount());
		System.out.println("number of public jobs: " + client.getJobCount(true));


		// show the oldest 10 jobs
		for (final JobHandle j : client.getJobList(Math.max(10, client.getJobCount()) - 10, 10))
			System.out.println(j);

		// show the most recently submitted job
		final JobHandle lastJob = client.getLastJob();
		System.out.println("Last job: " + lastJob);
		System.out.println("URL: " + lastJob.getUrl());
		System.out.println("Public URL: " + lastJob.getPublicUrl());
		System.out.println("Public? " + lastJob.getPublic());
		System.out.println("Source");
		System.out.println("---------------------");
		System.out.println(lastJob.getSource());
		System.out.println("---------------------");


		// create a new job by submitting a query and then do things with it
		final JobHandle j = client.query("o: output sum of int;\no << 1;");
		System.out.println("Submitted: " + j);

		j.stop();
		System.out.println("Stopped job: " + j);

		j.delete();
		System.out.println("Deleted job: " + j);


		// when finished, close the connection and log out of the remote server
		client.close();
		System.out.println("logged out");
	}
}
