/*
 * Copyright 2014, Robert Dyer.
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

import java.util.List;

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

		final BoaClient client = new BoaClient();

		client.login(args[0], args[1]);
		System.out.println("logged in");

		final List<InputHandle> datasets = client.getDatasets();
		for (final InputHandle d : datasets)
			System.out.println(d);

		final JobHandle lastJob = client.getLastJob();
		System.out.println("Last job submitted: " + lastJob);

		lastJob.stop();
		System.out.println("Stopped job: " + lastJob);

		client.close();
		System.out.println("logged out");
	}
}
