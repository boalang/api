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


		// show the most recently submitted job
		System.out.println("Last job: " + client.getLastJob());

		
		// create a new job by submitting a query and then do things with it
		final JobHandle j = client.query("o: output sum of int;\no << 1;");
		System.out.println("Submitted: " + j);

		j.stop();
		System.out.println("Stopped job: " + j);


		// when finished, close the connection and log out of the remote server
		client.close();
		System.out.println("logged out");
	}
}
