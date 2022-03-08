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
 * JackTripToInputBus is used to apply leveling, and a pre signal chain
 * to process audio from JackTrip and deliver it to the input audio bus
 *
 * \mix : array of amplitude level multipliers (default 1.0) for each client
 * \mul : master amplitude level multiplier (default 1.0)
 */

~synthDef = { | maxClients, preChain, postChain, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false |
    var signal;
    var defaultMix = 1 ! maxClients;
    var levels = \mix.kr(defaultMix) * \mul.kr(1.0);
    var offset = 0;

    signal = JackTripInput(maxClients, inputChannelsPerClient).getSignal();
    signal = preChain.ar(signal);
    signal = MulAdd(signal, levels, 0);

    if (withJamulus, {
        levels.removeAt(0);
        signal.removeAt(0);
        offset = 1;
    });

    signal.do({ arg item, i;
        Out.ar([~masterInputBus, ~inputBuses[i + offset].index], item);
    });
}