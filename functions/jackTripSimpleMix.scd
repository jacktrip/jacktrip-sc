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
 
"aggregateAndSendToAll.scd".loadRelative;
"sendFirstToEveryoneElse.scd".loadRelative;

 /*
 * jackTripSimpleMix: a minimal mix that scales well
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \inputChannelsPerClient: number of input channels received from each client
 * \outputChannelsPerClient: number of output channels sent to each client
 * \withJamulus: create mixes adapted Jamulus being connected on channels 1 & 2
 * \useSoundIn: if true, use SoundIn() for input channels; otherwise, use In()
 * \offset: input channel offset used for reading audio input
 * \levels : array of amplitude level multipliers for each client
 * \pan : array of pans for each client
 */

~jackTripSimpleMix = { | maxClients, levels, pan, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false, useSoundIn = true, offset = 0 |
    var signal = JackTripInput(maxClients, inputChannelsPerClient, useSoundIn, offset).getSignal();

	// adjust levels
	signal = MultiplyLink(levels).transform(signal);
    signal = SquashToMonoLink(true, false).transform(signal);
    signal = PanningLink(pan).transform(signal);

    if (withJamulus, {
        ~aggregateAndSendToAll.value(signal, maxClients, outputChannelsPerClient, 1);
        ~sendFirstToEveryoneElse.value(signal, maxClients, outputChannelsPerClient);
    }, {
        ~aggregateAndSendToAll.value(signal, maxClients, outputChannelsPerClient);
    });
};
