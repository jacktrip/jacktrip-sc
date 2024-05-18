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
 * JackTripSelfVolumeMixOut: uses subtractive synthesis to implement self volume
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \inputChannelsPerClient: number of input channels received from each client
 * \outputChannelsPerClient: number of output channels sent to each client
 * \withJamulus: create mixes adapted Jamulus being connected on channels 1 & 2
 * \mul : master amplitude level multiplier (default 1.0)
 * \extraSelfVolume : amount of additional volume multiplied by self
 * \in : input audio bus
 * \out : output audio bus
 */

~synthDef = { | maxClients, preChain, postChain, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false, broadcast = false |
    var masterSignal = JackTripInput(1, inputChannelsPerClient, false, ~masterInputBus).getSignal();
    var selfSignal = JackTripInput(1, inputChannelsPerClient, false, \in.ir(0)).getSignal();
    var speakerSignal = JackTripInput(1, inputChannelsPerClient, false, ~speakerInputBus).getSignal();

    var signal = masterSignal ++ selfSignal;
    signal = MulAdd(signal, [\mul.kr(1.0), \extraSelfVolume.kr(0)], 0);
    signal = AggregateLink().ar(signal);
    signal = postChain.ar(signal);

    speakerSignal = MulAdd(speakerSignal[0], \mul.kr(1.0), 0);
    speakerSignal = DelayN.ar(speakerSignal, 1.0, \speakerDelay.kr(0));
    signal = Mix([signal, speakerSignal]);

    Out.ar(\out.ir(0), signal);
};
