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
 
"../functions/jackTripSimpleMix.scd".loadRelative;

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

~synthDef = { | maxClients, preChain, postChain, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false, broadcast = false |
    var defaultMix = 1 ! maxClients;
    var levels = \mix.kr(defaultMix) * \mul.kr(1.0);
    ~jackTripSimpleMix.value(postChain, maxClients, levels, inputChannelsPerClient, outputChannelsPerClient, withJamulus, true, 0);
};
