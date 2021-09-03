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
 * JackTripSimpleIn is used to apply leveling and filters to a specific client input, 
 * and send it to an audio bus
 *
 * \low : low frequency to use for bandpass filter (default 20)
 * \high : high frequency to use for bandpass filter (default 20000)
 * \mix : array of amplitude level multipliers (default 1.0) for each client
 * \mul : master amplitude level multiplier (default 1.0)
  */

~synthDef = { | maxClients, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false |
    var signal;
    var levels = \mix.kr([1 ! maxClients]) * \mul.kr(1.0);

    signal = JackTripInput(maxClients, inputChannelsPerClient).getSignal();
    signal = BandPassFilterLink(\low.kr(20), \high.kr(20000)).transform(signal);
    signal = MultiplyLink(levels).transform(signal);

    signal.do({ arg item, i;
        Out.ar(~inputBuses[i].index, item);
    });
}