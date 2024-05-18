/* 
 * Copyright 2021-2022 JackTrip Labs, Inc.
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
 * JamulusDownMixOut: a minimal mix for Jamulus that scales well and reads from input busses
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \inputChannelsPerClient: number of input channels received from each client
 * \outputChannelsPerClient: number of output channels sent to each client
 * \mul : master amplitude level multiplier (default 1.0)
 */

~synthDef = { | maxClients, preChain, postChain, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false, broadcast = false |
    var signal, jamulusSignal;
    var speakerSignal = JackTripInput(1, inputChannelsPerClient, false, ~speakerInputBus).getSignal();
    
    signal = JackTripInput(1, inputChannelsPerClient, false, ~masterInputBus).getSignal();
    signal = MulAdd(signal[0], \mul.kr(1.0), 0);
    signal = postChain.ar(signal);

    speakerSignal = MulAdd(speakerSignal[0], \mul.kr(1.0), 0);
    speakerSignal = DelayN.ar(speakerSignal, 1.0, \speakerDelay.kr(0));
    signal = Mix([signal, speakerSignal]);

    // remove dry jamulus signal from the result to offset
    // the fact that jamulus will be echoing back to all users already
    // this ensures that they will hear the same thing as jacktrip users
    jamulusSignal = JackTripInput(1, inputChannelsPerClient, false, ~firstPrivateBus).getSignal();
    jamulusSignal = MulAdd(jamulusSignal[0], -1.0, signal);

    Out.ar(0, jamulusSignal);
};
