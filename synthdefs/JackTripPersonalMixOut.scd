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
 * JackTripPersonalMixOut is used to create a personal mix by combining output from the input buses
 *
 * \mul : amplitude level multiplier (default 1.0)
 */

~synthDef = { | maxClients, preChain, postChain, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false, broadcast = false |
    var defaultMixes = 1 ! (maxClients * maxClients);
    var personalMixes = \mix.kr(defaultMixes);
    var speakerSignal = JackTripInput(1, inputChannelsPerClient, false, ~speakerInputBus).getSignal();

    maxClients.do({ arg clientNum;
        // get personal mix
        var m = Array.fill(maxClients - 1, { arg n;
            if (maxClients == 1, {
                // work around weird supercollider bug where
                // an array of size 1 gets converted into a scalar
                // when used as a control rate parameter
                personalMixes;
            }, {
                personalMixes[(clientNum * maxClients) + n];
            });
        });
    
        // get input signals from private bus
        var signal = JackTripInput(maxClients - 1, outputChannelsPerClient, false, ~firstPrivateBus).getSignal();
        signal = MulAdd(signal, m * \mul.kr(1), 0);
        signal = AggregateLink().ar(signal);
        signal = postChain.ar(signal, clientNum);

        speakerSignal = MulAdd(speakerSignal, personalMixes[(clientNum + 1) * maxClients - 1] * \mul.kr(1), 0);
        speakerSignal = DelayN.ar(speakerSignal, 1.0, \speakerDelay.kr(0));
        signal = Mix([signal, speakerSignal]);
        
        Out.ar(outputChannelsPerClient * clientNum, signal);
    });
}