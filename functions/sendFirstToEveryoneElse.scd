/* 
 * Copyright 2021 JackTrip Labs, Inc.
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

/*
 * sendFirstToEveryoneElse: sends the input audio channels from the first
 * client to all other clients
 *
 * \in: array of input audio channels to aggregate together
 * \levels : array of amplitude level multipliers (default 1.0) for each client
 * \maxClients: maximum number of clients that may connect to the audio server
 * \outputChannelsPerClient: number of output channels sent to each client
 */
 
~sendFirstToEveryoneElse = { | in, levels, maxClients, outputChannelsPerClient = 2 |
	var out;

	// adjust levels
	var signal = MultiplyLink(levels).transform(in);

	// send Jamulus signal to JackTrip outputs
	out = Array.fill(maxClients - 1, { arg n;
		(n + 1) * outputChannelsPerClient;
	});
	Out.ar(out, signal[0]);
};
