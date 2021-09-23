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
 * aggregateAndSendToAll: aggregates an array of input signals
 * and sends audio to all clients
 *
 * \signal: array of input audio channels to aggregate together
 * \signalChain: used to process audio audio before delivery
 * \maxClients: maximum number of clients that may connect to the audio server
 * \outputChannelsPerClient: number of output channels sent to each client
 * \offset: number of signals to skip for aggregation
 */

~aggregateAndSendToAll = { | signal, signalChain, maxClients, outputChannelsPerClient = 2, offset = 0 |
	var out;

	// handle offset
	signal = (signal.size - offset).collect{ |n| signal[n + offset]; };

	// aggregate JackTrip signal
	signal = AggregateLink().transform(signal);
    signal = signalChain.value(signal);

	// send JackTrip signals to all outputs (including Jamulus)
	out = Array.fill(maxClients, { arg n;
		n * outputChannelsPerClient;
	});
	Out.ar(out, signal);
};
