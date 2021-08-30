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

~synthDefs = (

	/*
	 * JackTripSimpleIn is used to apply leveling and filters to a specific client input, 
	 * and send it to an audio bus
	 *
	 * \low : low frequency to use for bandpass filter (default 20)
	 * \high : high frequency to use for bandpass filter (default 20000)
	 * \mul : amplitude level multiplier (default 1.0)
	 */
	JackTripSimpleIn: { | maxClients, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false |
		var signal;

		signal = JackTripInput(maxClients, inputChannelsPerClient).getSignal();
		signal = BandPassFilterLink(\low.kr(20), \high.kr(20000)).transform(signal);
		signal = MultiplyLink(\mul.kr(1)).transform(signal);

		signal.do({ arg item, i;
			Out.ar(~inputBuses[i].index, item);
		});
	},

	/*
	 * JackTripPannedIn is used to apply leveling, filters and panning to a specific client input, 
	 * and send it to an audio bus
	 *
	 * \low : frequency to use for lowpass filter (default 20)
	 * \high : frequency to use for bandpass filter (default 20000)
	 * \mul : amplitude level multiplier (default 1.0)
	 */
	JackTripPannedIn: { | maxClients, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false |
		var signal;
		var defaultPan = 0 ! maxClients;

		signal = JackTripInput(maxClients, inputChannelsPerClient).getSignal();
		signal = BandPassFilterLink(\low.kr(20), \high.kr(20000)).transform(signal);
		signal = SquashToMonoLink(true, false).transform(signal);
		signal = PanningLink(\pan.kr(defaultPan)).transform(signal);
		signal = MultiplyLink(\mul.kr(1)).transform(signal);

		signal.do({ arg item, i;
			Out.ar(~inputBuses[i].index, item);
		});
	},

	/*
	 * JackTripPersonalMixOut is used to create a personal mix by combining output from the input buses
	 *
	 * \mul : amplitude level multiplier (default 1.0)
	 */
	JackTripPersonalMixOut: { | maxClients, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false |
		var personalMixes = \mix.kr(1 ! (maxClients * maxClients));

		maxClients.do({ arg clientNum;
			var signal = JackTripInput(maxClients, outputChannelsPerClient, false, ~firstPrivateBus).getSignal();
			var m = Array.fill(maxClients, { arg n;
				personalMixes[(clientNum * maxClients) + n];
			});
		
			var s = MultiplyLink(m * \mul.kr(1)).transform(signal);
			s = AggregateLink().transform(s);
		
			Out.ar(outputChannelsPerClient * clientNum, s);
		});

	},

	/*
	* JackTripSimpleMix: a minimal mix that scales well
	*
	* \maxClients: maximum number of clients that may connect to the audio server
	* \inputChannelsPerClient: number of input channels received from each client
	* \outputChannelsPerClient: number of output channels sent to each client
	* \withJamulus: create mixes adapted Jamulus being connected on channels 1 & 2
	* \mix : array of amplitude level multipliers (default 1.0) for each client
	* \mul : master amplitude level multiplier (default 1.0)
	*/
	JackTripSimpleMix: { | maxClients, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false |
		// TODO: replace reuse the definition inside of SimpleMix instead
		var levels = \mix.kr([1 ! maxClients]) * \mul.kr(1.0);
		if (withJamulus, {
			var jackTripSignal = JackTripInput(maxClients - 1, inputChannelsPerClient, true, (~firstPrivateBus + inputChannelsPerClient)).getSignal();
			var jamulusSignal = JackTripInput(1, inputChannelsPerClient, true, ~firstPrivateBus).getSignal();
			~aggregateAndSendToAll.value(jackTripSignal, levels, maxClients, outputChannelsPerClient);
			~sendFirstToEveryoneElse.value(jamulusSignal, levels, maxClients, outputChannelsPerClient);
		}, {
			var jackTripSignal = JackTripInput(maxClients, inputChannelsPerClient, true, ~firstPrivateBus).getSignal();
			~aggregateAndSendToAll.value(jackTripSignal, levels, maxClients, outputChannelsPerClient);
		});
	}	
);
