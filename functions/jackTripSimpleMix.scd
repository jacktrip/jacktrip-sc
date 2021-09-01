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
 * \offset: input channel offset used for reading audio input
 * \mix : array of amplitude level multipliers (default 1.0) for each client
 * \mul : master amplitude level multiplier (default 1.0)
 */

~jackTripSimpleMix = { | maxClients, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false, offset = 0 |
    var levels = \mix.kr([1 ! maxClients]) * \mul.kr(1.0);

    if (withJamulus, {
        var jackTripSignal = JackTripInput(maxClients - 1, inputChannelsPerClient, true, inputChannelsPerClient + offset).getSignal();
        var jamulusSignal = JackTripInput(1, inputChannelsPerClient, true, offset).getSignal();
        ~aggregateAndSendToAll.value(jackTripSignal, levels, maxClients, outputChannelsPerClient);
        ~sendFirstToEveryoneElse.value(jamulusSignal, levels, maxClients, outputChannelsPerClient);
    }, {
        var jackTripSignal = JackTripInput(maxClients, inputChannelsPerClient, true, offset).getSignal();
        ~aggregateAndSendToAll.value(jackTripSignal, levels, maxClients, outputChannelsPerClient);
    });
};
