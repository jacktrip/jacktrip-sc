/* 
 * Copyright 2020-2021 JackTrip Labs, Inc.
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
 * OutputBusMixer: creates a "JackTripDownMixOut" synth, which downmixes the
 * audio from all input busses and sends it to all clients.
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \preChain: signal processing chain applied to each client's audio before it is sent to the bus
 * \preChainName: unique name for the pre signal processing chain
 * \preChainArgs: array specifying initial values for the pre signal chain synth's arguments 
 * \postChain: signal processing chain applied to flattened audio before sending to each personal mix
 * \postChainName: unique name for the post signal processing chain
 * \postChainArgs: array specifying initial values for the post signal chain synth's arguments 
 */

OutputBusMixer : InputBusMixer {
	
	// the following parameters are instance variables
	// the '<' is shorthand for a getter method and '>' is shorthand for a setter method
	var <>postChainName = "";
	var <>postChainArgs = nil;

	// create a new instance
	*new { | maxClients = 16 |
		^super.new(maxClients);
	}

	// starts up all the audio on the server
	start {

		// start input bus mixer first
		super.start();

		Routine {
			var b, g, node, args;

			var synthName = "JackTripDownMixOut";

			// wait for server to be ready
			serverReady.wait;

			// use group 200 for client output synths
			g = ParGroup.basicNew(server, 200);

			// create a bundle of commands to execute
			b = server.makeBundle(nil, {
				this.sendSynthDef(synthName, synthName ++ postChainName);

				// use group 100 for client input synths and use group 200 for client output synths
				// p_new is a server command (see Server Command Reference on SC documentation)
				// that creates a parallel group, which represents a set of Synths that execute
				// simultaneously
				server.sendMsg("/p_new", 200, 1, 0);
			});

			// wait for server to receive bundle
			server.sync(nil, b);

			// create a single mix and outputs to all clients including jamulus
			args = [\mix, defaultMix, \mul, masterVolume] ++ postChainArgs;
			node = Synth(synthName ++ postChainName, args, g, \addToTail);
			("Created synth" + (synthName ++ postChainName) + node.nodeID).postln;

		}.run;
	}
}
