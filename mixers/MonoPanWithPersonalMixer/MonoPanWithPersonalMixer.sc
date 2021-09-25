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
 * MonoPanWithPersonalMixer: automatically pans clients across a stereo sound field,
 *                           and supports personal mixes up to 100 clients
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \panSlots: number of panning slots to use across a stereo sound field
 * \hpf: sets the default high-pass filter frequency used for all clients
 * \lpf: sets the default low-pass filter frequency used for all clients
 */

MonoPanWithPersonalMixer : PersonalMixer {
	
	// the following parameters are instance variables
	// the '<' is shorthand for a getter method and '>' is shorthand for a setter method
	var <>panSlots, <>hpf, <>lpf;

	// create a new instance
	*new { | maxClients = 16, panSlots = 1, hpf = 20, lpf = 20000 |
		^super.new(maxClients).panSlots_(panSlots).hpf_(hpf).lpf_(lpf);
	}

	// starts up all the audio on the server
	start {

		// prepare pre processing signal chain
		// squash to mono and pan clients
		this.preChain.clear().maxClients_(maxClients);
		this.preChain.append(BandPassFilterLink().low_(hpf).high_(lpf));
		this.preChain.append(SquashToMonoLink(true, false));
		this.preChain.append(PanningLink().maxClients_(maxClients).panSlots_(panSlots));

		// start InputBusMixer and PersonalMixer base classes
		super.start();

		Routine {
			// signal that the mix has started
			// signal is defined in the BaseMix class and represents a Condition object
			// after these two lines are executed, the BaseMix knows that the
			// proper Synths have been set up, and can execute other routines
			this.mixStarted.test = true;
			this.mixStarted.signal;
		}.run;
	}
}
