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
 * JamulusToInputBus is used to apply leveling, and a pre signal chain
 * to process audio from Jamulus and deliver it to the input audio bus
 *
 * \mul : master amplitude level multiplier (default 1.0)
 * \delay : delay added to dry jamulus signal, in seconds
 */

~synthDef = { | maxClients, preChain, postChain, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false |
    var defaultMix = 1 ! maxClients;
    var levels = \mix.kr(defaultMix) * \mul.kr(1.0);
    var drySignal, wetSignal;

    // get raw jamulus signal, and add delay if necessary (for broadcast)
    drySignal = JackTripInput(1, inputChannelsPerClient).getSignal();
    drySignal = DelayN.ar(drySignal, 1.0, \jamulusDelay.kr(0));

    // generate wet signal by applying effects, and send to master input bus
    wetSignal = preChain.ar(drySignal);
    wetSignal = MulAdd(wetSignal, levels[0], 0);
    Out.ar(~masterInputBus, wetSignal[0]);

    // send dry jamulus signal to the private input bus so that
    // it can later be subtracted from the wet signal
    Out.ar(~firstPrivateBus, drySignal[0]);
}
